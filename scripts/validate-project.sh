#!/usr/bin/env bash
set -euo pipefail
python - <<'PY'
import json
from pathlib import Path
root=Path('app/src/main/assets')
files=list(root.rglob('*.json'))
for path in files:
    json.loads(path.read_text(encoding='utf-8'))
    print('OK', path)

expert=json.loads((root/'deep/expert_engineering.json').read_text(encoding='utf-8'))
scenarios=json.loads((root/'deep/scenarios.json').read_text(encoding='utf-8'))
tracks=expert['tracks']
lesson_count=sum(len(t.get('lessons',[])) for t in tracks)
scenario_count=len(scenarios.get('scenarios',[]))
core=json.loads((root/'curriculum.json').read_text(encoding='utf-8'))
core_levels=core.get('levels',[]) if isinstance(core,dict) else core
core_lessons=sum(len(x.get('lessons',[])) for x in core_levels if isinstance(x,dict))
assert len(tracks)==32, f'Expected 32 deep tracks, got {len(tracks)}'
assert lesson_count==160, f'Expected 160 deep lessons, got {lesson_count}'
assert scenario_count==160, f'Expected 160 scenarios, got {scenario_count}'
assert core_lessons>=600, f'Core lesson pack unexpectedly small: {core_lessons}'

master=json.loads((root/'master/netmaster_master_curriculum.json').read_text(encoding='utf-8'))
labs=json.loads((root/'master/lab_catalog.json').read_text(encoding='utf-8'))
ai=json.loads((root/'master/ai_playbooks.json').read_text(encoding='utf-8'))
assert len(master.get('phases',[]))==12
assert len(labs.get('labs',[]))==48
assert len(ai.get('modes',{}))==7
print(f'Deep content: {len(tracks)} tracks / {lesson_count} lessons / {scenario_count} scenarios; core lessons={core_lessons}; master phases={len(master["phases"])}; labs={len(labs["labs"])}; ai modes={len(ai["modes"])}')
PY

test -x ./gradlew
test -s gradle/wrapper/gradle-wrapper.jar
test -s gradle/wrapper/gradle-wrapper.properties
test -s .github/workflows/android.yml
test -s codemagic.yaml
printf 'Project validation: OK\n'
