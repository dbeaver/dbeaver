#!/usr/bin/env python3
import argparse
from pathlib import Path
import subprocess
import tempfile
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--jdk', required=True, type=Path)
parser.add_argument('--asm', required=True, type=Path)
parser.add_argument('--annotations', required=True, type=Path)
parser.add_argument('--output', required=True, type=Path)
args = parser.parse_args()
source = Path(__file__).parent / 'src'
args.output.parent.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix='dbeaver-scale-agent-') as temporary:
    classes = Path(temporary)
    subprocess.run([
        str(args.jdk / 'bin/javac'), '--release', '21', '-encoding', 'UTF-8', '-proc:none',
        '-cp', str(args.asm) + ':' + str(args.annotations), '-d', str(classes),
        *map(str, source.rglob('*.java'))
    ], check=True)
    with zipfile.ZipFile(args.output, 'w', zipfile.ZIP_DEFLATED) as output:
        output.writestr('META-INF/MANIFEST.MF',
                        'Manifest-Version: 1.0\nPremain-Class: org.jkiss.dbeaver.poc.scale.MacOsScaleAgent\n\n')
        for path in classes.rglob('*.class'):
            output.write(path, path.relative_to(classes))
        with zipfile.ZipFile(args.asm) as dependency:
            for entry in dependency.namelist():
                if entry.startswith('org/objectweb/asm/') and entry.endswith('.class'):
                    output.writestr(entry, dependency.read(entry))
print(args.output)
