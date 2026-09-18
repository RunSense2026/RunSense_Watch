# RunSense_Watch

런센스 전용 갤럭시 워치(Wear OS) 앱입니다. 갤럭시 워치 5/6에서 내장 심박 센서·GPS로 러닝을 추적하고,
런센스 백엔드와 AI 코칭 서버를 호출하며, 블루투스로 페어링된 폰 앱(RunSense_FrontEnd)과 값을 주고받습니다.

## 화면 흐름

```
01 대기 ─[시작]→ 02 센서 확인 ─[자동]→ 03 러닝 중 ⇄ 04 심박 ⇄ 05 폼 요약 ⇄ 06 AI 코치
                                          │ ─[코칭 도착]→ 06 AI 코치 (6초 후 복귀)
                                          └─[‖]→ 07 일시정지 ─[종료]→ 08 러닝 완료 ─[확인]→ 01
```

| 화면 | 데이터 출처 |
| --- | --- |
| 01 대기 | 심박 권한/지원 여부, 폰 연결 여부, 폰이 보낸 로그인 이름, 인솔 연결 상태 |
| 02 센서 확인 | 인솔 L/R(폰 → Data Layer), 워치 GPS(Health Services 예열). 없으면 "미연결"로 표시하고 단독 시작 |
| 03 러닝 중 | Health Services ExerciseClient(RUNNING): 거리·속도(페이스)·심박·케이던스·칼로리. 가장자리 링 = 페이스 vs 목표/평균 |
| 04 심박 | 심박 + LTHR 기반 5존(폰/서버 프로필의 LTHR, 없으면 220-나이) + 존별 체류 막대 |
| 05 폼 요약 | 케이던스(워치) + 좌우 균형·뒤꿈치 착지율(폰 인솔) → 폼 점수 |
| 06 AI 코치 | `POST /api/run/coaching`(로그인 시) → 실패/비로그인 시 `POST /ai/coaching`. mp3 음성은 워치 스피커, 실패 시 TTS |
| 07 일시정지 | ExerciseClient pause/resume |
| 08 완료 | `POST /api/run` 저장(로그인 시) → 실패하면 워치에 보관 후 다음 실행 때 재전송. 카드 탭 → 폰 앱 리포트 열기 |

## 서버

`app/build.gradle.kts` 의 `BuildConfig` 로 주입합니다.

- 백엔드: `https://runsense-backend-server-9he7.onrender.com/` (JWT, 401/403 시 `refreshToken` 으로 자동 재발급)
- AI 코칭: `https://runsense-ai-server.onrender.com/` (인증 없음, 콜드 스타트로 1분 이상 걸릴 수 있어 타임아웃 120초)

## 폰 앱 연동 (Wearable Data Layer)

**두 앱은 같은 `applicationId`(`com.runsense`)와 같은 서명 키**여야 Data Layer 가 연결됩니다.
이 저장소는 폰 앱의 `android/app/debug.keystore` 복사본(`app/runsense-debug.keystore`)으로 debug/release 를 서명합니다.
폰 앱은 `Galaxy Wearable` 로 페어링된 워치에 워치 앱을 설치(또는 `adb -s <watch> install`)하면 됩니다.

경로/페이로드 규격: 워치 `data/sync/PhoneSyncContract.kt` ↔ 폰 `src/wear/wearBridge.ts`.

| 방향 | 경로 | 내용 |
| --- | --- | --- |
| 폰 → 워치 | `/runsense/auth` | 로그인 토큰(accessToken/refreshToken/userId/userName). 로그아웃 시 삭제 + `/runsense/auth/clear` |
| 폰 → 워치 | `/runsense/profile` | LTHR·안정시 심박·생년월일·키·체중 (LTHR 은 서버에 없고 폰에만 있음) |
| 폰 → 워치 | `/runsense/insole/status` | 인솔 L/R 연결 여부 |
| 폰 → 워치 | `/runsense/insole/data` | 러닝 중 1초마다 양발 족압(AI 서버 FootData 스키마) |
| 폰 → 워치 | `/runsense/run/phone` | 폰 러닝 화면 열림 여부 + 목표 페이스. 열려 있으면 워치는 저장·코칭을 폰에 맡김 |
| 폰 → 워치 | `/runsense/coach` | 폰이 만든 AI 코칭 문장 (워치 화면에 표시, 음성은 폰) |
| 워치 → 폰 | `/runsense/run/state` | started / paused / resumed / ended |
| 워치 → 폰 | `/runsense/run/live` | 5초마다 심박·케이던스·거리·페이스·폼 점수 → 폰 대시보드가 시뮬레이션 대신 사용 |
| 워치 → 폰 | `/runsense/run/complete` | 요약 + 서버 저장 결과(activityId) |
| 워치 → 폰 | `/runsense/open_report` | 완료 화면 카드 탭 → 폰 앱 리포트 화면 |
| 워치 → 폰 | `/runsense/insole/request` | 센서 확인 화면에서 인솔 상태 재요청 |

## 개발/테스트

- 폰트: Pretendard(가변, KS X 1001 한글 서브셋) + Rajdhani(숫자). `res/font/`
- 에뮬레이터에서 센서 없이 테스트하려면 Health Services 합성 데이터를 켭니다.

```
adb shell am broadcast -a "whs.USE_SYNTHETIC_PROVIDERS" com.google.android.wearable.healthservices
adb shell am broadcast -a "whs.synthetic.user.START_RUNNING" com.google.android.wearable.healthservices
```

- 실제 워치(개발자 옵션 → ADB 디버깅/무선 디버깅)에 설치: `./gradlew :app:installDebug`
