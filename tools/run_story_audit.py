"""Native dedicated-server regression runner; requires JDK 21 and Gradle dependencies.
Never places test entry points in the production source set or release JAR.
"""

import argparse
import json
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

r = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser()
p.add_argument(
    "--accept-eula",
    action="store_true",
    help="Accept the Minecraft EULA for this temporary test server",
)
args = p.parse_args()
if not args.accept_eula:
    p.error(
        "Read https://aka.ms/MinecraftEULA and pass --accept-eula to run the temporary server."
    )
root = r / ".audit"
root.mkdir(exist_ok=True)
output = r / "build/reports/story"
output.mkdir(parents=True, exist_ok=True)
with tempfile.TemporaryDirectory(prefix="story-", dir=root) as temp:
    t = Path(temp)
    src = t / "src/com/timelocked"
    src.mkdir(parents=True)
    for name in (
        "IntroAudit.java",
        "CampusOpeningAudit.java",
        "SccCampusAudit.java",
        "NeighborhoodAudit.java",
        "CampusRebuildAudit.java",
        "SccPhotoAudit.java",
        "LightEngineAudit.java",
        "CoastChapterAudit.java",
        "CoastalTruckAudit.java",
        "VehicleAudioAudit.java",
        "ObjectiveHudAudit.java",
        "AtlasBootAudit.java",
        "FacilitySpawnAudit.java",
        "GlassStairwellAudit.java",
        "ReceptionChapterAudit.java",
        "StaffChapterAudit.java",
        "ResearchStoryAudit.java",
        "HubLightingAudit.java",
        "WakePresentationAudit.java",
        "TransitionDoorwaysAudit.java",
    ):
        shutil.copy(r / "validation" / name, src / name)
    run = t / "run"
    run.mkdir()
    (run / "eula.txt").write_text("eula=true\n", encoding="utf-8")
    (run / "server.properties").write_text(
        "online-mode=false\nserver-ip=127.0.0.1\nserver-port=0\nlevel-seed=8918\nview-distance=2\nsimulation-distance=2\nmax-tick-time=-1\nspawn-protection=0\n",
        encoding="utf-8",
    )

    def q(path):
        return json.dumps(str(path))

    init = t / "audit.init.gradle"
    init.write_text(
        "allprojects { afterEvaluate {\n"
        + f'layout.buildDirectory = file({q(t / "build")})\nsourceSets.main.java.srcDir({q(t / "src")})\nneoForge.runs.server.gameDirectory = file({q(run)})\nneoForge.runs.server.jvmArguments.addAll "-Xms256m", "-Xmx2G"\nneoForge.runs.server.systemProperty "timelocked.audit.output", {q(output)}\n'
        + "}}\n",
        encoding="utf-8",
    )
    report = output / "native-story-audit.json"
    report.unlink(missing_ok=True)
    command = (
        [str(r / "gradlew.bat")]
        if sys.platform == "win32"
        else ["bash", str(r / "gradlew")]
    ) + [
        "-I",
        str(init),
        "runServer",
        "--console=plain",
        "--no-configuration-cache",
        "-Dorg.gradle.jvmargs=-Xmx768m",
    ]
    log = output / "story-audit-run.log"
    with log.open("w") as stream:
        result = subprocess.run(command, cwd=r, stdout=stream, stderr=subprocess.STDOUT)
    if result.returncode or not report.exists():
        sys.exit(
            "Audit failed to complete. See build/reports/story/story-audit-run.log"
        )
    data = json.loads(report.read_text(encoding="utf-8"))
    print(
        f"{data['status']}: {data['checks']} native checks; {len(data['failures'])} failures"
    )
    if data["failures"]:
        print("\n".join(data["failures"]))
        sys.exit(1)
