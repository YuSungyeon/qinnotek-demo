#!/usr/bin/env bash
#
# EC2(Ubuntu)에 LibreOffice headless + 한글 폰트 설치.
# 워드/한글/엑셀/파워포인트를 PDF 사본으로 변환하기 위해 필요하다.
#
# 사용법:
#   bash deploy/libreoffice-setup.sh
#
# 주의:
#   - 한글 폰트가 없으면 변환은 "성공"하지만 PDF 글자가 전부 두부(□□□)로 나온다.
#     그래서 스크립트 마지막에 한글 변환 스모크 테스트를 반드시 돌린다.
#   - 설치 용량이 500MB~1GB 다. 먼저 df -h 로 여유를 확인한다.
set -euo pipefail

echo "==> 0) 디스크 여유 확인"
df -h / | tail -1

echo "==> 1) LibreOffice + 한글 폰트 설치"
sudo apt-get update -y
sudo apt-get install -y --no-install-recommends \
  libreoffice-core \
  libreoffice-writer \
  libreoffice-calc \
  libreoffice-impress \
  libreoffice-java-common \
  fonts-nanum fonts-nanum-extra fonts-noto-cjk
sudo fc-cache -f

echo "==> 2) 설치 확인"
soffice --version

echo "==> 3) 한글 폰트 확인"
if ! fc-list :lang=ko | head -3 | grep -q .; then
  echo "경고: 한글 폰트가 잡히지 않습니다. PDF 가 두부(□)로 나올 수 있습니다."
fi
fc-list :lang=ko | head -3

echo "==> 4) 한글 변환 스모크 테스트"
SMOKE=$(mktemp -d)
trap 'rm -rf "${SMOKE}"' EXIT

# fodt(Flat ODF)는 단일 XML 이라 별도 도구 없이 만들 수 있다
cat >"${SMOKE}/smoke.fodt" <<'FODT'
<?xml version="1.0" encoding="UTF-8"?>
<office:document xmlns:office="urn:oasis:names:tc:opendocument:xmlns:office:1.0"
                 xmlns:text="urn:oasis:names:tc:opendocument:xmlns:text:1.0"
                 office:version="1.2" office:mimetype="application/vnd.oasis.opendocument.text">
  <office:body><office:text>
    <text:p>사업자등록증 한글 변환 확인 ABC 123</text:p>
  </office:text></office:body>
</office:document>
FODT

soffice --headless --invisible --nologo --nodefault --norestore --nolockcheck \
  -env:UserInstallation="file://${SMOKE}/profile" \
  --convert-to pdf --outdir "${SMOKE}" "${SMOKE}/smoke.fodt" >/dev/null 2>&1

if [ ! -s "${SMOKE}/smoke.pdf" ]; then
  echo "실패: PDF 가 생성되지 않았습니다."
  exit 1
fi
echo "PDF 생성 성공: $(du -h "${SMOKE}/smoke.pdf" | cut -f1)"

# 텍스트 추출로 한글이 살아있는지 검증 (pdftotext 가 있을 때만)
if command -v pdftotext >/dev/null 2>&1; then
  if pdftotext "${SMOKE}/smoke.pdf" - 2>/dev/null | grep -q "사업자등록증"; then
    echo "한글 텍스트 확인 완료 - 폰트 정상"
  else
    echo "경고: PDF 에서 한글을 찾지 못했습니다. 폰트 설치를 다시 확인하세요."
  fi
else
  echo "참고: poppler-utils 가 없어 텍스트 검증을 건너뜁니다 (sudo apt-get install -y poppler-utils)"
fi

echo ""
echo "==> 5) (선택) HWPX 지원 확장 - H2Orestart"
echo "스톡 LibreOffice 는 구형 .hwp 만 부분 지원하고 .hwpx 는 열지 못한다."
echo "필요하면 아래를 수동 실행하세요 (실패해도 앱은 원본만 저장하는 방식으로 동작합니다):"
echo "  wget https://github.com/ebandal/H2Orestart/releases/latest/download/H2Orestart.oxt"
echo "  sudo unopkg add --shared H2Orestart.oxt"
echo "  sudo systemctl restart photo-submission"

echo ""
echo "완료. /etc/photo-submission.env 에 SOFFICE_BIN=$(command -v soffice) 를 넣어두면 탐지가 확실해집니다."
