import React, {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {inMemoryPersistence, setPersistence, signInWithCustomToken, signOut} from 'firebase/auth';
import {collection, getDocsFromServer, type DocumentData} from 'firebase/firestore';
import {httpsCallable} from 'firebase/functions';
import {deleteToken, getMessaging, getToken, isSupported, onMessage} from 'firebase/messaging';
import {firebase} from './firebase';
import './styles.css';

type Role = 'parent' | 'teacher';
type Section = 'attendance' | 'payments' | 'grades' | 'notifications';
type Student = {id: string; name: string; code: string};
type Group = {id: string; name: string; students: Student[]};
type RecordData = DocumentData & {id: string};
const sections: Section[] = ['attendance', 'payments', 'grades', 'notifications'];
const labels = {
  attendance: ['الحضور', 'Attendance'], payments: ['المدفوعات', 'Payments'],
  grades: ['درجات الامتحانات', 'Exam grades'], notifications: ['الإشعارات', 'Notifications'],
};
const digits = (value: string) => value.replace(/[٠-٩۰-۹]/g, c => String(c.charCodeAt(0) & 15));
const worker = 'serviceWorker' in navigator ? navigator.serviceWorker.register('/firebase-messaging-sw.js').catch(() => null) : Promise.resolve(null);
async function call<T>(name: string, data = {}) {
  if (!firebase) throw Error('Firebase configuration required');
  return (await httpsCallable<unknown, T>(firebase.functions, name)(data)).data;
}
const isIOS = /iPhone|iPad|iPod/.test(navigator.userAgent) || (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

function App() {
  const [ar, setArabic] = useState(true);
  const [role, setRole] = useState<Role>('parent');
  const [phone, setPhone] = useState('');
  const [pin, setPin] = useState('');
  const [session, setSession] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const [device, setDevice] = useState('');
  const [notice, setNotice] = useState('');
  const t = (arabic: string, english: string) => ar ? arabic : english;

  useEffect(() => {
    document.documentElement.lang = ar ? 'ar' : 'en';
    document.documentElement.dir = ar ? 'rtl' : 'ltr';
  }, [ar]);
  useEffect(() => {
    if (!device || !firebase) return;
    return onMessage(getMessaging(firebase.app), payload => setNotice(payload.notification?.title ?? ''));
  }, [device]);

  async function logIn(event: React.FormEvent) {
    event.preventDefault();
    if (!firebase || busy) return;
    setBusy(true);
    setError('');
    try {
      const result = await call<{token: string; phone: string}>('portalLogin', {role, phone, pin});
      await setPersistence(firebase.auth, inMemoryPersistence);
      await signInWithCustomToken(firebase.auth, result.token);
      setSession(result.phone);
      setPin('');
    } catch (failure) {
      const code = (failure as {code?: string}).code;
      setError(code === 'functions/resource-exhausted'
        ? t('محاولات كثيرة. حاول بعد ١٥ دقيقة.', 'Too many attempts. Try again in 15 minutes.')
        : t('تعذر الدخول. تحقق من الرقم والرمز والاتصال.', 'Sign-in failed. Check your phone, PIN, and connection.'));
    } finally { setBusy(false); }
  }

  async function enableNotifications() {
    if (!firebase || busy) return;
    setBusy(true);
    setError('');
    try {
      if (!('Notification' in window)) throw Error();
      if (await Notification.requestPermission() !== 'granted' || !await isSupported()) throw Error();
      const registration = await worker;
      if (!registration || !import.meta.env.VITE_FIREBASE_VAPID_KEY) throw Error();
      const token = await getToken(getMessaging(firebase.app), {
        serviceWorkerRegistration: await navigator.serviceWorker.ready, vapidKey: import.meta.env.VITE_FIREBASE_VAPID_KEY,
      });
      if (!token) throw Error();
      await call('portalDevice', {token});
      setDevice(token);
      setNotice(t('تم تفعيل الإشعارات', 'Notifications enabled'));
    } catch { setError(t('تعذر تفعيل الإشعارات على هذا الجهاز.', 'Notifications could not be enabled on this device.')); }
    finally { setBusy(false); }
  }

  async function leave() {
    if (!firebase || busy) return;
    setBusy(true);
    setError('');
    try {
      if (device) {
        try { await call('portalDevice', {token: device, remove: true}); }
        catch { await deleteToken(getMessaging(firebase.app)); }
      }
      await signOut(firebase.auth);
      setSession('');
      setPhone('');
      setDevice('');
      setNotice('');
    } catch { setError(t('تعذر إنهاء الجلسة. حاول مرة أخرى.', 'Could not sign out. Try again.')); }
    finally { setBusy(false); }
  }

  return <main className="mx-auto max-w-3xl space-y-5 px-4 py-6">
    <header className="flex items-center justify-between gap-3">
      <div className="flex items-center gap-3"><img src="/icon-192.png" className="h-12 w-12 rounded-xl" alt="" />
        <h1 className="text-2xl font-bold">{t('سنتر وان', 'Center One')}</h1></div>
      <div className="flex flex-wrap gap-2"><button className="secondary" onClick={() => setArabic(!ar)}>{ar ? 'English' : 'العربية'}</button>
      {session && <button className="secondary" disabled={busy} onClick={leave}>{t('خروج', 'Sign out')}</button>}</div>
    </header>
    {!firebase && <p role="alert" className="panel">{t('البوابة قيد الإعداد.', 'The portal is awaiting Firebase configuration.')}</p>}
    {isIOS && !window.matchMedia('(display-mode: standalone)').matches && !(navigator as Navigator & {standalone?: boolean}).standalone &&
      <aside className="rounded-xl bg-amber-100 p-4">{t('لتفعيل الإشعارات، افتح قائمة المشاركة في سفاري واضغط «إضافة إلى الشاشة الرئيسية»، ثم افتح التطبيق.', 'To enable notifications, tap Share → Add to Home Screen in Safari, then open the installed app.')}</aside>}
    {error && <p role="alert" className="text-red-800">{error}</p>}
    {notice && <p role="status" className="text-teal-800">{notice}</p>}
    {!session ? <section className="panel space-y-4">
      <div className="flex flex-wrap gap-2" aria-label={t('نوع الحساب', 'Account type')}>
        <button className={role === 'parent' ? '' : 'secondary'} aria-pressed={role === 'parent'} disabled={busy} onClick={() => {setRole('parent'); setPin(''); setError('');}}>{t('ولي الأمر', 'Parent')}</button>
        <button className={role === 'teacher' ? '' : 'secondary'} aria-pressed={role === 'teacher'} disabled={busy} onClick={() => {setRole('teacher'); setPin(''); setError('');}}>{t('المعلم', 'Teacher')}</button>
      </div>
      <form onSubmit={logIn} className="grid gap-3">
        <label>{t('رقم الهاتف', 'Phone number')}<input dir="ltr" type="tel" autoComplete="tel" maxLength={30} required value={phone} onChange={e => setPhone(e.target.value)} /></label>
        <label>{t('رمز الدخول (٦ أرقام)', 'Access PIN (6 digits)')}<input dir="ltr" type="password" autoComplete="current-password" inputMode="numeric" pattern="[0-9]{6}" maxLength={6} required value={pin} onChange={e => setPin(digits(e.target.value).replace(/[^0-9]/g, ''))} /></label>
        <button disabled={!firebase || busy}>{busy ? t('جارٍ الدخول…', 'Signing in…') : t('دخول', 'Sign in')}</button>
      </form>
    </section> : role === 'parent' ? <>
      <button className="secondary" onClick={enableNotifications} disabled={busy || !!device}>{device ? t('الإشعارات مفعلة', 'Notifications enabled') : t('تفعيل الإشعارات', 'Enable notifications')}</button>
      <ParentPortal phone={session} ar={ar} />
    </> : <TeacherPortal ar={ar} />}
  </main>;
}

function ParentPortal({phone, ar}: {phone: string; ar: boolean}) {
  const [data, setData] = useState<Record<'children' | Section, RecordData[]> | null>(null);
  const [childId, setChildId] = useState('');
  const [section, setSection] = useState<Section>('attendance');
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [revision, refresh] = useState(0);
  const t = (arabic: string, english: string) => ar ? arabic : english;
  useEffect(() => {
    if (!firebase) return;
    const db = firebase.db;
    let current = true;
    const paths = ['children', ...sections] as const;
    setLoading(true);
    setFailed(false);
    Promise.all(paths.map(path => getDocsFromServer(collection(db, 'parents', phone, path))))
      .then(results => {
        if (!current) return;
        const records = Object.fromEntries(results.map((result, i) => [
          paths[i], result.docs.map(doc => ({...doc.data(), id: doc.id})),
        ])) as Record<'children' | Section, RecordData[]>;
        setData(records);
        setChildId(previous => records.children.some(child => child.id === previous) ? previous : records.children[0]?.id ?? '');
      })
      .catch(() => { if (current) {setData(null); setFailed(true);} })
      .finally(() => { if (current) setLoading(false); });
    return () => {current = false;};
  }, [phone, revision]);

  const child = data?.children.find(child => child.id === childId);
  const date = (value: unknown, withTime = false) => typeof value === 'number'
    ? new Intl.DateTimeFormat(ar ? 'ar-EG' : 'en-GB', {timeZone: 'Africa/Cairo', dateStyle: 'medium', ...(withTime ? {timeStyle: 'medium'} : {})}).format(value)
    : String(value ?? '—');
  const money = (value: number) => new Intl.NumberFormat(ar ? 'ar-EG' : 'en-GB', {style: 'currency', currency: 'EGP'}).format(value);
  const records = data?.[section].filter(record => record.studentId === child?.studentId).sort((a, b) =>
    Number(b.timestamp ?? b.checkInTime ?? b.date ?? 0) - Number(a.timestamp ?? a.checkInTime ?? a.date ?? 0)) ?? [];

  return <section className="panel space-y-4">
    <button className="secondary" disabled={loading} onClick={() => refresh(revision + 1)}>{t('تحديث', 'Refresh')}</button>
    {loading && <p role="status">{t('جارٍ التحميل…', 'Loading…')}</p>}
    {failed && <p role="alert">{t('تعذر التحميل. تحقق من الاتصال أو أعد تسجيل الدخول.', 'Could not load. Check your connection or sign in again.')}</p>}
    {data && !child && <p>{t('لا توجد بيانات طلاب بعد.', 'No students are available yet.')}</p>}
    {child && <>
      {data!.children.length > 1 && <div className="flex flex-wrap gap-2">{data!.children.map(item =>
        <button key={item.id} className={item.id === childId ? '' : 'secondary'} aria-pressed={item.id === childId} onClick={() => setChildId(item.id)}>{item.name}</button>)}</div>}
      <div><h2 className="text-xl font-bold">{child.name}</h2><p>{child.grade}</p><p>{(child.activeGroups ?? []).join(' · ')}</p></div>
      <nav className="flex flex-wrap gap-2" aria-label={t('بيانات الطالب', 'Student records')}>
        {sections.map(tab => <button key={tab} className={tab === section ? '' : 'secondary'} aria-pressed={tab === section} onClick={() => setSection(tab)}>{labels[tab][ar ? 0 : 1]}</button>)}
      </nav>
      {section === 'payments' && <div className="rounded-xl bg-amber-50 p-3">
        <p>{t('الرسوم الشهرية', 'Monthly fee')} · {money(child.monthlyDue)}</p>
        <p>{t('المبلغ المتأخر', 'Overdue')} · {money(child.overdueAmount)}</p>
      </div>}
      {records.length === 0 && <p>{t('لا توجد سجلات بعد.', 'No records yet.')}</p>}
      <ul className="divide-y divide-stone-200">{records.map(record => <li className="space-y-1 py-3" key={record.id}>
        {section === 'attendance' && <><p>{t('الدخول', 'Check-in')} · {date(record.checkInTime, true)}</p><p>{t('الخروج', 'Check-out')} · {date(record.checkOutTime, true)}</p></>}
        {section === 'payments' && <><strong>{money(record.amount)}</strong><p>{record.month} · {date(record.date)} · {t('مؤكد', 'Confirmed')}</p></>}
        {section === 'grades' && <><strong>{record.examTitle}</strong><p>{record.groupName} · <bdi>{record.score} / {record.maxScore}</bdi> · {date(record.date)}</p>{record.notes && <p>{record.notes}</p>}</>}
        {section === 'notifications' && <><strong>{ar ? record.title : ({CHECK_IN:'Check-in', CHECK_OUT:'Check-out', PAYMENT_CONFIRMED:'Payment confirmed', OVERDUE_REMINDER:'Overdue payment', EXAM_GRADE:'New exam grade'} as Record<string, string>)[record.type] ?? 'Center One'}</strong>{ar && <p>{record.body}</p>}<p>{date(record.timestamp, true)}</p></>}
      </li>)}</ul>
    </>}
  </section>;
}

function TeacherPortal({ar}: {ar: boolean}) {
  const [groups, setGroups] = useState<Group[]>([]);
  const [groupId, setGroupId] = useState('');
  const [examTitle, setTitle] = useState('');
  const [maximum, setMaximum] = useState('');
  const [scores, setScores] = useState<Record<string, string>>({});
  const [error, setError] = useState('');
  const [saved, setSaved] = useState(0);
  const [busy, setBusy] = useState(false);
  const [loadFailed, setLoadFailed] = useState(false);
  const [upload, setUpload] = useState<{signature: string; ids: string[]} | null>(null);
  const t = (arabic: string, english: string) => ar ? arabic : english;
  useEffect(() => {
    let current = true;
    call<Group[]>('portalTeacherGroups').then(result => {
      if (current) {setGroups(result); setGroupId(result[0]?.id ?? '');}
    }).catch(() => {if (current) setLoadFailed(true);});
    return () => {current = false;};
  }, []);
  const group = groups.find(item => item.id === groupId);
  async function importScores(file: File) {
    if (!group || busy) return;
    setError('');
    setBusy(true);
    try {
      if (file.size > 2_000_000) throw Error();
      let parsed: string[][];
      if (file.name.toLowerCase().endsWith('.xlsx')) {
        const {default: readExcel} = await import('read-excel-file');
        parsed = (await readExcel(file)).map(row => row.map(cell => String(cell ?? '')));
      } else {
        const {default: Papa} = await import('papaparse');
        parsed = await new Promise<string[][]>((resolve, reject) => Papa.parse<string[]>(file, {
          skipEmptyLines: 'greedy', complete: result => result.errors.length ? reject(Error()) : resolve(result.data), error: reject,
        }));
      }
      const [header, ...rows] = parsed;
      const normalized = header?.map(value => value.trim().replace(/^\uFEFF/, '').toLowerCase()) ?? [];
      const index = (...names: string[]) => normalized.findIndex(value => names.includes(value));
      const codeIndex = index('code', 'studentcode', 'الكود', 'كود');
      const nameIndex = index('name', 'studentname', 'الاسم', 'اسم');
      const scoreIndex = index('score', 'الدرجة', 'درجة');
      if ((codeIndex < 0 && nameIndex < 0) || scoreIndex < 0 || !rows.length) throw Error();
      const imported: Record<string, string> = {};
      for (const row of rows) {
        if (row.every(cell => !cell.trim())) continue;
        const code = row[codeIndex]?.trim();
        const name = row[nameIndex]?.trim();
        const matches = group.students.filter(student => code ? student.code === code : student.name === name);
        const score = digits(row[scoreIndex]?.trim() ?? '');
        if (matches.length !== 1 || matches[0].id in imported || !/^\d+(?:\.\d+)?$/.test(score)) throw Error();
        imported[matches[0].id] = score;
      }
      setScores(imported);
      setSaved(0);
    } catch { setError(t('تعذر الاستيراد. تحقق من أعمدة الكود أو الاسم والدرجة ومن عدم تكرار الطلاب.', 'Import failed. Check the code or name and score columns, and look for duplicate or unmatched students.')); }
    finally { setBusy(false); }
  }
  async function save(event: React.FormEvent) {
    event.preventDefault();
    if (!group || busy) return;
    setError('');
    setSaved(0);
    const maxScore = Number(maximum);
    const rows = Object.entries(scores).filter(([, score]) => score !== '').map(([studentId, score]) => ({studentId, score: Number(score)}));
    if (!examTitle.trim() || !Number.isFinite(maxScore) || maxScore <= 0 || !rows.length || rows.some(row => !Number.isFinite(row.score) || row.score < 0 || row.score > maxScore)) {
      setError(t('تحقق من اسم الامتحان والدرجات.', 'Enter an exam name and valid scores.')); return;
    }
    const signature = JSON.stringify({groupId, examTitle, maxScore, rows});
    const chunkSize = 150;
    const chunks = Array.from({length: Math.ceil(rows.length / chunkSize)}, (_, i) =>
      rows.slice(i * chunkSize, (i + 1) * chunkSize));
    const ids = upload?.signature === signature ? upload.ids : chunks.map(() => crypto.randomUUID());
    setUpload({signature, ids});
    setBusy(true);
    let submitted = 0;
    try {
      for (const [index, chunk] of chunks.entries()) {
        const result = await call<{saved: number}>('portalSaveGrades', {groupId, examTitle, maxScore, rows: chunk, uploadId: ids[index]});
        submitted += result.saved;
      }
      setSaved(submitted);
      setScores({});
      setUpload(null);
    } catch {
      setError(submitted > 0
        ? t(`تم حفظ ${submitted} درجة. أعد المحاولة لإكمال الحفظ.`, `${submitted} grades saved. Retry to finish saving.`)
        : t('تعذر حفظ الدرجات. تحقق من الاتصال وعضوية الطلاب في المجموعة.', 'Could not save grades. Check the connection and group membership.'));
    }
    finally { setBusy(false); }
  }
  return <form className="panel grid gap-4" onSubmit={save}>
    <h2 className="text-xl font-bold">{t('درجات الامتحانات', 'Exam grades')}</h2>
    {loadFailed && <p role="alert">{t('تعذر تحميل المجموعات. أعد تسجيل الدخول.', 'Could not load groups. Sign in again.')}</p>}
    {!loadFailed && groups.length === 0 && <p>{t('ستظهر المجموعات هنا بعد المزامنة.', 'Groups appear here after synchronization.')}</p>}
    <label>{t('المجموعة', 'Group')}<select value={groupId} disabled={busy || !groups.length} onChange={e => {setGroupId(e.target.value); setScores({}); setSaved(0);}}>{groups.map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
    <label>{t('اسم الامتحان', 'Exam name')}<input required disabled={busy} maxLength={160} value={examTitle} onChange={e => setTitle(e.target.value)} /></label>
    <label>{t('الدرجة النهائية', 'Maximum score')}<input required disabled={busy} type="number" step="any" min="0" value={maximum} onChange={e => setMaximum(e.target.value)} /></label>
    <label>{t('استيراد CSV أو Excel', 'Import CSV or Excel')}<input type="file" accept=".csv,.xlsx" disabled={busy || !group} onChange={e => {const file = e.target.files?.[0]; if (file) void importScores(file); e.target.value = '';}} /></label>
    {group?.students.map(student => <label key={student.id} className="grid grid-cols-[1fr_7rem] items-center gap-2 border-t border-stone-200 pt-2">
      <span>{student.name} · <bdi>{student.code}</bdi></span><input aria-label={t('درجة ', 'Score for ') + student.name} disabled={busy} type="number" step="any" min="0" max={maximum || undefined} value={scores[student.id] ?? ''} onChange={e => setScores({...scores, [student.id]: e.target.value})} />
    </label>)}
    {error && <p role="alert" className="text-red-800">{error}</p>}
    {saved > 0 && <p role="status">{t('تم حفظ الدرجات', 'Grades saved')} · {saved}</p>}
    <button disabled={busy || !group}>{t('حفظ الدرجات', 'Save grades')}</button>
  </form>;
}

createRoot(document.getElementById('root')!).render(<App />);
