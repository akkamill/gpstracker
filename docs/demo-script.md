# Demo script (about 3 minutes)

## Before the presentation

1. Start Docker Desktop, then build and start the server:
   `./gradlew :server:installDist` and `docker compose up --build -d`.
2. Start the emulator, run `adb reverse tcp:8080 tcp:8080`, and install the app (`./gradlew installDebug`).
3. Open <http://localhost:8080/support> in a browser and enter the key `local-support-key`.
4. Put the emulator and the browser side by side.

## On stage

1. **The problem (20 s).** Drivers do not know how they drive or what harsh driving costs. A
   tracker in the car sends its data; here a simulated car plays that role.
2. **Sign in (15 s).** Sign in with `demo@drivemetrics.app` / `demo1234`. The account's eight
   weeks of trips come from the server.
3. **Drive (60 s).** Nobody presses start: the trip starts when the car moves. Point out the speed
   limit, the score dropping after a harsh braking, and the warning about the police report
   ahead. Answer "Still there?" and report a closed road with one tap.
4. **Map (15 s).** The route, the driving events and every report, including the one just made.
5. **Trip saved (20 s).** The trip ends by itself; open it: where points were lost, fuel wasted, tips.
6. **Insights (15 s).** Score over eight weeks against the benchmark driver.
7. **Support (25 s).** Settings → Chat with support. Write a message; answer it from the browser.
8. **Quality (10 s).** Every module is tested (unit, UI, database and PostgreSQL tests), and CI
   checks style, code smells and coverage on every push.

If something fails live: **Try the demo** works without the server.
