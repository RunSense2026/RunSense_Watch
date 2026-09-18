// 폰 앱 로고(RunSense_FrontEnd/src/assets/runsense-logo.png)로 워치 런처 아이콘을 만든다.
// 폰 앱(scripts/gen-launcher-icons.js)과 같은 로고·흰 배경을 써서 두 앱 아이콘을 통일한다.
//
// 사용: node scripts/gen-launcher-icons.js   (jimp 는 폰 저장소의 node_modules 를 빌려 쓴다)
//
// 생성물
//  - drawable-*/ic_launcher_foreground.png : 적응형 아이콘 전경 (108dp 캔버스, 안전 영역 66% 안에 로고)
//  - mipmap-*/ic_launcher.png, ic_launcher_round.png : 레거시 아이콘 (폰과 동일 규격)
const path = require('path');
const fs = require('fs');

const ROOT = path.resolve(__dirname, '..');
const PHONE = path.resolve(ROOT, '../RunSense_FrontEnd');
const Jimp = require(path.join(PHONE, 'node_modules/jimp'));
const SRC = path.join(PHONE, 'src/assets/runsense-logo.png');
const RES = path.join(ROOT, 'app/src/main/res');

const LEGACY = { mdpi: 48, hdpi: 72, xhdpi: 96, xxhdpi: 144, xxxhdpi: 192 };
const ADAPTIVE = { mdpi: 108, hdpi: 162, xhdpi: 216, xxhdpi: 324, xxxhdpi: 432 };

(async () => {
  const logo = await Jimp.read(SRC);
  // 로고 PNG 는 흰 배경이 아니라 투명일 수 있으니 흰 캔버스 위에 올려 폰 아이콘과 같은 모양을 만든다.
  const onWhite = (size) => {
    const canvas = new Jimp(size, size, 0xffffffff);
    return canvas.composite(logo.clone().resize(size, size), 0, 0);
  };

  for (const [dpi, size] of Object.entries(LEGACY)) {
    const dir = path.join(RES, `mipmap-${dpi}`);
    fs.mkdirSync(dir, { recursive: true });
    const square = onWhite(size);
    await square.writeAsync(path.join(dir, 'ic_launcher.png'));

    const round = square.clone();
    const r = size / 2;
    round.scan(0, 0, size, size, (x, y, idx) => {
      const dx = x - r + 0.5;
      const dy = y - r + 0.5;
      if (dx * dx + dy * dy > r * r) round.bitmap.data[idx + 3] = 0;
    });
    await round.writeAsync(path.join(dir, 'ic_launcher_round.png'));
    console.log(`mipmap-${dpi}: ${size}px ✓`);
  }

  for (const [dpi, size] of Object.entries(ADAPTIVE)) {
    const dir = path.join(RES, `drawable-${dpi}`);
    fs.mkdirSync(dir, { recursive: true });
    // 적응형 아이콘은 바깥 18dp 가 잘려 나가므로 로고를 캔버스의 74% 크기로 가운데에 둔다.
    // (로고 PNG 자체에 여백이 있어 실제 신발은 안전 영역 66% 안에 들어온다)
    const inner = Math.round(size * 0.74);
    const canvas = new Jimp(size, size, 0x00000000);
    canvas.composite(logo.clone().resize(inner, inner), Math.round((size - inner) / 2), Math.round((size - inner) / 2));
    await canvas.writeAsync(path.join(dir, 'ic_launcher_foreground.png'));
    console.log(`drawable-${dpi}: ${size}px ✓`);
  }
  console.log('완료');
})().catch((e) => {
  console.error(e);
  process.exit(1);
});
