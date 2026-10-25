import shutil, pathlib
PKG = 'com.vibe.jarvis'
root = pathlib.Path('android/app/src/main')
java = root / 'java' / PKG.replace('.', '/')
java.mkdir(parents=True, exist_ok=True)
for f in pathlib.Path('android-src/java').glob('*.java'):
    shutil.copy(f, java / f.name)
(root / 'res/xml').mkdir(parents=True, exist_ok=True)
(root / 'res/values').mkdir(parents=True, exist_ok=True)
shutil.copy('android-src/res/xml/voice_interaction_service.xml', root / 'res/xml/')
shutil.copy('android-src/res/values/assist_styles.xml', root / 'res/values/')
m = root / 'AndroidManifest.xml'
s = m.read_text()
if 'AssistActivity' not in s:
    s = s.replace('</application>', pathlib.Path('android-src/manifest_application.xml').read_text() + '\n</application>')
    s = s.replace('<application', pathlib.Path('android-src/manifest_permissions.xml').read_text() + '\n<application', 1)
    m.write_text(s)
print('patched')
