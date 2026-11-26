import {createHash, randomUUID, timingSafeEqual} from 'node:crypto';
import {initializeApp} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {FieldValue, getFirestore} from 'firebase-admin/firestore';
import {getMessaging} from 'firebase-admin/messaging';
import {onCall, HttpsError} from 'firebase-functions/v2/https';
import {onDocumentCreated} from 'firebase-functions/v2/firestore';

initializeApp();
const db = getFirestore();
const hash = value => createHash('sha256').update(value).digest('hex');
const invalid = () => new HttpsError('invalid-argument', 'Invalid portal request');
const denied = () => new HttpsError('permission-denied', 'Portal access denied');
const digits = value => String(value ?? '').replace(/[٠-٩۰-۹]/g, c => String(c.charCodeAt(0) & 15));
const phoneNumber = value => {
  const number = digits(value).replace(/[ ()+.\-]/g, '');
  if (/^(?:0020|20)1[0125][0-9]{8}$/.test(number)) return '0' + number.slice(-10);
  return /^[0-9]{7,15}$/.test(number) ? number : '';
};
const collection = role => role === 'parent' ? 'parents' : 'teachers';

export const portalLogin = onCall(async request => {
  const {role, phone, pin} = request.data ?? {};
  const number = phoneNumber(phone);
  const candidate = digits(pin);
  if (!['parent', 'teacher'].includes(role) || !number || !/^[0-9]{6}$/.test(candidate)) throw invalid();

  const now = Date.now();
  const windowMs = 15 * 60_000;
  const keys = [hash(role + ':' + number), hash('ip:' + (request.rawRequest.ip || 'unknown'))];
  const limits = keys.map(key => db.doc('portalLoginLimits/' + key));
  await db.runTransaction(async tx => {
    const snapshots = await tx.getAll(...limits);
    snapshots.forEach((snapshot, index) => {
      const window = Math.floor(now / windowMs);
      const attempts = snapshot.get('window') === window ? snapshot.get('attempts') : 0;
      if (attempts >= (index === 0 ? 8 : 60)) throw new HttpsError('resource-exhausted', 'Try again later');
      tx.set(limits[index], {window, attempts: attempts + 1});
    });
  });

  const account = await db.doc(collection(role) + '/' + number).get();
  const expected = account.get(role === 'parent' ? 'accessPin' : 'portalPin');
  const same = timingSafeEqual(Buffer.from(hash(String(expected ?? ''))), Buffer.from(hash(candidate)));
  if (!same || account.get('active') !== true) throw denied();
  const portalUntil = now + 12 * 60 * 60_000;
  const portalGeneration = account.get('generation');
  const token = await getAuth().createCustomToken(hash(role + ':' + number), {
    portalPhone: number, portalRole: role, portalUntil, portalGeneration,
  });
  return {token, phone: number};
});

function session(request, role) {
  const claims = request.auth?.token;
  if (claims?.portalRole !== role || !(claims.portalUntil > Date.now())) throw denied();
  return {ref: db.doc(collection(role) + '/' + claims.portalPhone), generation: claims.portalGeneration};
}

function authorized(snapshot, generation) {
  if (snapshot.get('active') !== true || snapshot.get('generation') !== generation) throw denied();
}

export const portalDevice = onCall(async request => {
  const {ref, generation} = session(request, 'parent');
  const {token, remove} = request.data ?? {};
  if (typeof token !== 'string' || token.length < 20 || token.length > 4096) throw invalid();
  const device = db.doc('portalDevices/' + hash(token));
  await db.runTransaction(async tx => {
    const [account, prior] = await tx.getAll(ref, device);
    authorized(account, generation);
    if (remove) {
      if (prior.get('path') === ref.path) tx.delete(device);
      tx.update(ref, {fcmTokens: FieldValue.arrayRemove(token)});
      return;
    }
    const tokens = account.get('fcmTokens') ?? [];
    if (tokens.length >= 50 && !tokens.includes(token)) throw new HttpsError('resource-exhausted', 'Too many devices');
    const previousPath = prior.get('path');
    if (previousPath && previousPath !== ref.path) {
      const previous = await tx.get(db.doc(previousPath));
      if (previous.exists) tx.update(previous.ref, {fcmTokens: FieldValue.arrayRemove(token)});
    }
    tx.set(device, {path: ref.path});
    tx.update(ref, {fcmTokens: FieldValue.arrayUnion(token)});
  });
});

export const portalTeacherGroups = onCall(async request => {
  const {ref, generation} = session(request, 'teacher');
  const account = await ref.get();
  authorized(account, generation);
  return (account.get('groups') ?? []).map(({id, name, students}) => ({
    id, name, students: students.map(({id, name, code}) => ({id, name, code})),
  }));
});

export const portalSaveGrades = onCall(async request => {
  const {ref, generation} = session(request, 'teacher');
  const {groupId, examTitle, maxScore, rows, uploadId} = request.data ?? {};
  const title = typeof examTitle === 'string' ? examTitle.trim() : '';
  if (typeof groupId !== 'string' || !title || title.length > 160 ||
      typeof maxScore !== 'number' || !Number.isFinite(maxScore) || maxScore <= 0 ||
      typeof uploadId !== 'string' || !/^[\da-f-]{36}$/i.test(uploadId) ||
      !Array.isArray(rows) || rows.length < 1 || rows.length > 150 ||
      new Set(rows.map(row => row?.studentId)).size !== rows.length ||
      rows.some(row => typeof row?.studentId !== 'string' || typeof row.score !== 'number' ||
        !Number.isFinite(row.score) || row.score < 0 || row.score > maxScore)) throw invalid();

  const receipt = db.doc('portalGradeUploads/' + hash(ref.path + ':' + uploadId));
  await db.runTransaction(async tx => {
    const [account, previous] = await tx.getAll(ref, receipt);
    authorized(account, generation);
    if (previous.exists) return;
    const group = (account.get('groups') ?? []).find(group => group.id === groupId);
    if (!group) throw denied();
    const records = rows.map(({studentId, score}) => {
      const student = group.students.find(student => student.id === studentId);
      if (!student?.parentPhone) throw denied();
      const parent = db.doc('parents/' + student.parentPhone);
      return {student, score, parent, child: parent.collection('children').doc(student.childDocumentId ?? studentId)};
    });
    const children = await tx.getAll(...records.map(record => record.child));
    const date = Date.now();
    records.forEach(({student, score, parent}, index) => {
      if (!children[index].get('activeGroupIds')?.includes(groupId)) throw denied();
      const id = randomUUID();
      const grade = {studentId: student.id, studentName: student.name, groupId, groupName: group.name,
        examTitle: title, score, maxScore, date, notes: '', updatedAt: date, source: 'portal'};
      tx.create(parent.collection('grades').doc(id), grade);
      tx.create(parent.collection('notifications').doc('grade-' + id), {
        title: 'درجة امتحان جديدة', body: 'تتوفر درجة امتحان جديدة في بوابة سنتر وان.',
        type: 'EXAM_GRADE', studentId: student.id, studentName: student.name, timestamp: date, source: 'portal',
      });
      tx.create(db.doc('portalGrades/' + id), grade);
    });
    tx.create(receipt, {date});
  });
  return {saved: rows.length};
});

export const notifyPortalGrade = onDocumentCreated('parents/{phone}/notifications/{id}', async event => {
  const notification = event.data?.data();
  if (notification?.source !== 'portal') return;
  const parent = db.doc('parents/' + event.params.phone);
  const account = await parent.get();
  const tokens = account.get('active') === true ? account.get('fcmTokens') ?? [] : [];
  if (!tokens.length) return;
  const result = await getMessaging().sendEachForMulticast({
    tokens, notification: {title: notification.title, body: notification.body},
  });
  const invalidTokens = tokens.filter((_, index) => [
    'messaging/registration-token-not-registered', 'messaging/invalid-registration-token',
  ].includes(result.responses[index].error?.code));
  if (invalidTokens.length) await parent.update({fcmTokens: FieldValue.arrayRemove(...invalidTokens)});
});
