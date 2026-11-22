"""
One Gradle build at a time for the whole team (agents share one checkout and the PC has 16 GB).

Usage (from anywhere):
  python design-reference/kirox-v3/qa/kxbuild.py compile          -> :app:compileDebugKotlin
  python design-reference/kirox-v3/qa/kxbuild.py test "com.kirox.app.v3.*"
  python design-reference/kirox-v3/qa/kxbuild.py install          -> installV3preview on the Xiaomi only
  python design-reference/kirox-v3/qa/kxbuild.py raw <gradle args...>

Waits for the shared lock, runs Gradle with the team JDK, prints only errors/warnings summary
and the exit code. Never run ./gradlew directly while this team is working.
"""
import os, subprocess, sys, time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..', '..'))
LOCK = os.path.join(ROOT, 'build', 'kxbuild.lock')
JDK = os.path.join(os.environ.get('LOCALAPPDATA', ''), 'Programs', 'Eclipse Adoptium', 'jdk-21.0.9.10-hotspot')

def acquire():
    os.makedirs(os.path.dirname(LOCK), exist_ok=True)
    waited = 0
    while True:
        try:
            fd = os.open(LOCK, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.write(fd, f'{os.getpid()} {time.time()}'.encode()); os.close(fd)
            return
        except FileExistsError:
            try:  # stale lock (> 20 min) is removed
                if time.time() - os.path.getmtime(LOCK) > 1200: os.remove(LOCK); continue
            except OSError: pass
            if waited % 60 == 0: print(f'[kxbuild] waiting for another build ({waited}s)…', flush=True)
            time.sleep(5); waited += 5

def main():
    if len(sys.argv) < 2: print(__doc__); return 2
    cmd, rest = sys.argv[1], sys.argv[2:]
    env = dict(os.environ, JAVA_HOME=JDK)
    if cmd == 'compile': args = [':app:compileDebugKotlin']
    elif cmd == 'test': args = [':app:testDebugUnitTest'] + (['--tests', rest[0]] if rest else [])
    elif cmd == 'install': args = [':app:installV3preview']; env['ANDROID_SERIAL'] = '79ed61c9'
    elif cmd == 'raw': args = rest
    elif cmd == 'rawfull':
        if any('install' in a.lower() for a in rest): print('rawfull never installs; use install'); return 2
        args = rest
    else: print(__doc__); return 2
    gradlew = os.path.join(ROOT, 'gradlew.bat' if os.name == 'nt' else 'gradlew')
    acquire()
    try:
        p = subprocess.run([gradlew] + args + (['--offline'] if cmd == 'rawfull' else ['--offline', '-q']), cwd=ROOT, env=env, capture_output=True, text=True, encoding='utf-8', errors='replace')
        out = (p.stdout or '') + (p.stderr or '')
        ol = out.splitlines(); lines = []
        for i, l in enumerate(ol):
            if l.startswith('e: ') or 'file:///' in l or 'FAILED' in l or 'tests completed' in l: lines.append(l)
            if 'What went wrong' in l: lines.extend(ol[i:i + 6])
        print(out[-60000:] if cmd == 'rawfull' else ('\n'.join(lines[:60]) if lines else out[-3000:]))
        print(f'[kxbuild] exit {p.returncode}')
        return p.returncode
    finally:
        try: os.remove(LOCK)
        except OSError: pass

if __name__ == '__main__':
    sys.exit(main())
