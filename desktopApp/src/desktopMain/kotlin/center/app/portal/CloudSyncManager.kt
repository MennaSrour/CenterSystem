package center.app.portal

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.nio.file.Path
import java.util.prefs.Preferences

internal data class CloudSettings(
    val keyPath: String = "",
    val automatic: Boolean = false,
)

internal data class CloudStatus(
    val pending: Long = 0,
    val online: Boolean = false,
    val busy: Boolean = false,
)

internal class CloudSyncManager(
    private val store: PortalStore,
    private val openCloud: (Path) -> PortalCloud = ::FirebasePortal,
    initialSettings: CloudSettings = CloudSettings(),
    private val saveSettings: (CloudSettings) -> Unit = {},
) {
    private val lock = Mutex()
    private val mutableSettings = MutableStateFlow(initialSettings)
    val settings = mutableSettings.asStateFlow()
    private val mutableStatus = MutableStateFlow(CloudStatus())
    val status = mutableStatus.asStateFlow()

    fun configure(settings: CloudSettings) {
        saveSettings(settings)
        mutableSettings.value = settings
        mutableStatus.value = mutableStatus.value.copy(online = false)
    }

    suspend fun run() {
        while (currentCoroutineContext().isActive) {
            if (settings.value.automatic && settings.value.keyPath.isNotBlank()) {
                sync()
            } else {
                refreshPending()
            }
            delay(30_000)
        }
    }

    suspend fun testConnection(): Boolean =
        withContext(Dispatchers.IO) {
            val path = settings.value.keyPath.ifBlank { return@withContext false }
            try {
                openCloud(Path.of(path)).use { it.testConnection(store.installationId()) }
                true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                false
            }
        }

    suspend fun sync() =
        withContext(Dispatchers.IO) {
            if (!lock.tryLock()) return@withContext
            try {
                val config = settings.value
                mutableStatus.value = mutableStatus.value.copy(busy = true)
                val pending = store.pendingCount()
                if (config.keyPath.isBlank()) {
                    mutableStatus.value = CloudStatus(pending)
                    return@withContext
                }
                openCloud(Path.of(config.keyPath)).use { cloud ->
                    store.importPortalGrades(cloud.downloadGrades(store.installationId()))
                    val snapshot = store.snapshot(System.currentTimeMillis())
                    cloud.publish(snapshot.projection)
                    if (settings.value.keyPath == config.keyPath) store.acknowledge(snapshot)
                }
                mutableStatus.value = CloudStatus(store.pendingCount(), online = settings.value.keyPath == config.keyPath)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableStatus.value = CloudStatus(mutableStatus.value.pending)
            } finally {
                mutableStatus.value = mutableStatus.value.copy(busy = false)
                lock.unlock()
            }
            refreshPending()
        }

    private suspend fun refreshPending() =
        withContext(Dispatchers.IO) {
            lock.withLock {
                try {
                    store.initializePins()
                    mutableStatus.value = mutableStatus.value.copy(pending = store.pendingCount())
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    mutableStatus.value = mutableStatus.value.copy(online = false)
                }
            }
        }
}

internal fun cloudSyncManager(store: PortalStore): CloudSyncManager {
    val preferences = runCatching { Preferences.userRoot().node("center-one/cloud-sync") }.getOrNull()

    fun read(prefs: Preferences?) =
        CloudSettings(
            prefs?.get("serviceAccountPath", "").orEmpty(),
            prefs?.getBoolean("automatic", false) ?: false,
        )
    return CloudSyncManager(
        store,
        initialSettings = runCatching { read(preferences) }.getOrDefault(CloudSettings()),
        saveSettings = { settings ->
            if (preferences != null) {
                runCatching {
                    preferences.put("serviceAccountPath", settings.keyPath)
                    preferences.putBoolean("automatic", settings.automatic)
                    preferences.flush()
                }
            }
        },
    )
}
