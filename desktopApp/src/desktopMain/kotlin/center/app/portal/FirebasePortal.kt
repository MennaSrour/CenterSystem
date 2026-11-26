package center.app.portal

import com.google.api.core.ApiFuture
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.DocumentSnapshot
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.MulticastMessage
import com.google.firebase.messaging.Notification
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit

internal interface PortalCloud : AutoCloseable {
    fun testConnection(installationId: String)

    fun downloadGrades(installationId: String): Map<String, Fields>

    fun publish(projection: PortalProjection)
}

internal class FirebasePortal(
    key: Path,
) : PortalCloud {
    private val app =
        FirebaseApp.initializeApp(
            FirebaseOptions
                .builder()
                .setCredentials(Files.newInputStream(key).use(GoogleCredentials::fromStream))
                .setConnectTimeout(8_000)
                .setReadTimeout(12_000)
                .build(),
            "center-one-${UUID.randomUUID()}",
        )
    private val firestore: Firestore by lazy { FirestoreClient.getFirestore(app) }

    override fun testConnection(installationId: String) {
        val owner = firestore.document("portalConfig/owner").get().await()
        check(!owner.exists() || owner.getString("installationId") == installationId)
    }

    override fun downloadGrades(installationId: String): Map<String, Fields> {
        val owner = firestore.document("portalConfig/owner")
        firestore
            .runTransaction { transaction ->
                val current = transaction.get(owner).await()
                if (current.exists()) {
                    check(current.getString("installationId") == installationId) { "A different desktop database owns the portal" }
                } else {
                    transaction.create(owner, mapOf("installationId" to installationId))
                }
            }.await()
        return firestore
            .collection("portalGrades")
            .get()
            .await()
            .documents
            .associate { it.id to it.data }
    }

    override fun publish(projection: PortalProjection) {
        val existingParents =
            firestore
                .collection("parents")
                .get()
                .await()
                .documents
                .associateBy { it.id }
        val existingTeachers =
            firestore
                .collection("teachers")
                .get()
                .await()
                .documents
                .associateBy { it.id }
        deactivate(existingParents, projection.parents.keys, parent = true)
        deactivate(existingTeachers, projection.teachers.keys, parent = false)

        val existingDocuments =
            sections
                .flatMap { section ->
                    firestore.collectionGroup(section).get().await().documents.filter { record ->
                        val path = record.reference.path.split('/')
                        path.size == 4 && path[0] == "parents" && path[1] in existingParents
                    }
                }.associateBy { it.reference.path }
        val childMembership =
            projection.documents
                .asSequence()
                .filter { (path, _) -> path.contains("/children/") }
                .map { (path, document) -> path.substringBefore("/children/") to document.fields.text("studentId") }
                .toSet()
        val stale =
            existingDocuments.filter { (path, record) ->
                if (path in projection.documents) return@filter false
                val studentId = record.getString("studentId")
                val parentPath =
                    record.reference.parent.parent
                        ?.path
                val childStillHere = (parentPath to studentId) in childMembership
                record.getString("source") != "portal" || !childStillHere
            }
        stale.values.chunked(450).forEach { records ->
            val batch = firestore.batch()
            records.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }

        projection.parents.forEach { (phone, account) ->
            updateAccount("parents", phone, "accessPin", account, existingParents[phone])
        }
        val changes =
            projection.documents.filter { (path, document) ->
                val remote = existingDocuments[path]
                remote?.getString("source") != "portal" && remote?.getString("revision") != portalHash(document.fields.toString())
            }
        val pendingPush =
            existingDocuments
                .filter { (path, record) ->
                    path !in stale && path.contains("/notifications/") && record.getBoolean("pendingPush") == true
                }.keys
                .toMutableSet()
        changes.entries.chunked(400).forEach { batchEntries ->
            val batch = firestore.batch()
            batchEntries.forEach { (path, document) ->
                val fields = document.fields + mapOf("revision" to portalHash(document.fields.toString()), "source" to "desktop")
                val shouldPush = document.push && path.contains("/notifications/") && path !in existingDocuments
                if (shouldPush) pendingPush.add(path)
                batch.set(firestore.document(path), if (shouldPush) fields + ("pendingPush" to true) else fields, SetOptions.merge())
            }
            batch.commit().await()
        }
        projection.teachers.forEach { (phone, account) ->
            updateAccount("teachers", phone, "portalPin", account, existingTeachers[phone])
        }

        pendingPush.groupBy { it.substringBefore("/notifications/") }.forEach { (parentPath, notifications) ->
            val parent = firestore.document(parentPath)
            val tokens = (parent.get().await().get("fcmTokens") as? List<*>)?.filterIsInstance<String>().orEmpty().distinct()
            val invalidTokens = mutableSetOf<String>()
            notifications.forEach { path ->
                invalidTokens.addAll(deliver(path, tokens.filterNot(invalidTokens::contains)))
            }
            if (invalidTokens.isNotEmpty()) parent.update("fcmTokens", FieldValue.arrayRemove(*invalidTokens.toTypedArray())).await()
        }
    }

    private fun deactivate(
        accounts: Map<String, DocumentSnapshot>,
        active: Set<String>,
        parent: Boolean,
    ) {
        accounts
            .filter { (phone, account) -> phone !in active && account.getBoolean("active") == true }
            .values
            .chunked(400)
            .forEach { stale ->
                val batch = firestore.batch()
                stale.forEach { account ->
                    val changes =
                        if (parent) {
                            mapOf("active" to false, "fcmTokens" to emptyList<String>())
                        } else {
                            mapOf("active" to false, "groups" to emptyList<Any>())
                        }
                    batch.update(account.reference, changes)
                }
                batch.commit().await()
            }
    }

    private fun updateAccount(
        collection: String,
        id: String,
        pinField: String,
        account: PortalAccount,
        previous: DocumentSnapshot?,
    ) {
        val revision = portalHash(account.fields.toString())
        val newIdentity =
            previous?.getBoolean("active") != true ||
                previous.getString(pinField) != account.pin ||
                (collection == "teachers" && previous.getString("teacherId") != account.fields["teacherId"])
        if (!newIdentity && previous?.getString("revision") == revision) return
        val fields =
            (
                account.fields +
                    mapOf(
                        pinField to account.pin,
                        "active" to true,
                        "revision" to revision,
                        "updatedAt" to FieldValue.serverTimestamp(),
                    )
            ).toMutableMap()
        if (newIdentity) fields["generation"] = UUID.randomUUID().toString()
        // Merge keeps device tokens written by parents, including registrations made during sync.
        if (collection == "parents" && previous == null) fields["fcmTokens"] = emptyList<String>()
        firestore
            .collection(collection)
            .document(id)
            .set(fields, SetOptions.merge())
            .await()
    }

    private fun deliver(
        path: String,
        tokens: List<String>,
    ): Set<String> {
        val messaging = FirebaseMessaging.getInstance(app)
        val dropped = mutableSetOf<String>()
        tokens.chunked(500).forEach { batch ->
            val result =
                messaging
                    .sendEachForMulticastAsync(
                        MulticastMessage
                            .builder()
                            .addAllTokens(batch)
                            .setNotification(
                                Notification
                                    .builder()
                                    .setTitle("سنتر وان")
                                    .setBody("يتوفر تحديث جديد في بوابة سنتر وان.")
                                    .build(),
                            ).build(),
                    ).await()
            val invalid =
                result.responses.mapIndexedNotNull { index, response ->
                    when (response.exception?.messagingErrorCode) {
                        MessagingErrorCode.UNREGISTERED, MessagingErrorCode.INVALID_ARGUMENT -> batch[index]
                        else -> null
                    }
                }
            dropped.addAll(invalid)
            check(result.failureCount == invalid.size) { "FCM delivery requires a retry" }
        }
        firestore.document(path).update("pendingPush", FieldValue.delete()).await()
        return dropped
    }

    override fun close() = app.delete()

    private companion object {
        val sections = listOf("children", "attendance", "payments", "grades", "notifications")
    }
}

private fun <T> ApiFuture<T>.await(): T =
    try {
        get(20, TimeUnit.SECONDS)
    } catch (failure: Exception) {
        cancel(true)
        throw failure
    }
