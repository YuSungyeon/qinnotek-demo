<script setup>
import { computed, ref, watch, onBeforeUnmount } from 'vue'
import { absoluteUrl } from '../api/client'
import { acceptFor, acceptsKind, kindOf } from '../lib/filekind'
import DocPreview from './DocPreview.vue'
import Icon from './Icon.vue'

const props = defineProps({
  item: { type: Object, required: true },
  index: { type: Number, default: 0 },
  /**
   * 이 항목에 선택된 것 (부모 관리 - 직접 선택/자동 분류 모두 이 prop으로 반영)
   * { kind: 'photo', file } | { kind: 'doc', staged }
   */
  selection: { type: Object, default: null },
  /** AI 자동 배정 여부 (확인 필요 표시) */
  auto: { type: Boolean, default: false },
  /** 위치 바꾸기 버튼 노출 여부 (항목이 2개 이상일 때) */
  swappable: { type: Boolean, default: false }
})
const emit = defineEmits(['select', 'select-doc', 'zoom', 'swap'])

const cameraInput = ref(null)
const galleryInput = ref(null)
const docInput = ref(null)
const previewUrl = ref('')
const error = ref('')

const fileType = computed(() => props.item.fileType || 'PHOTO')
const allowsPhoto = computed(() => fileType.value !== 'DOCUMENT')
const allowsDoc = computed(() => fileType.value === 'DOCUMENT' || fileType.value === 'ANY')
const stagedDoc = computed(() => (props.selection?.kind === 'doc' ? props.selection.staged : null))
const filled = computed(() => !!props.selection)

// 사진 선택 → 미리보기 URL 동기화 (문서는 URL 을 만들지 않는다)
watch(
  () => (props.selection?.kind === 'photo' ? props.selection.file : null),
  (f) => {
    if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
    previewUrl.value = f ? URL.createObjectURL(f) : ''
  },
  { immediate: true }
)

function pickCamera() {
  cameraInput.value?.click()
}
function pickGallery() {
  galleryInput.value?.click()
}
function pickDoc() {
  docInput.value?.click()
}

function onFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (!file) return

  const kind = kindOf(file)
  if (kind === 'unknown') {
    error.value = '지원하지 않는 파일 형식이에요.'
    return
  }
  if (!acceptsKind(fileType.value, kind)) {
    error.value =
      fileType.value === 'DOCUMENT'
        ? '이 항목은 문서 파일만 올릴 수 있어요.'
        : '이 항목은 사진만 올릴 수 있어요.'
    return
  }
  error.value = ''
  if (kind === 'doc') {
    // 문서는 서버가 읽어야 해서 이 시점에 바로 올라간다
    emit('select-doc', { itemId: props.item.itemId, file })
  } else {
    // 사진은 즉시 업로드하지 않고 선택만 전달 (전송 시 일괄 업로드)
    emit('select', { itemId: props.item.itemId, file })
  }
}

onBeforeUnmount(() => {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
})
</script>

<template>
  <div
    class="item-card"
    :class="{ done: filled, returned: item.status === 'RETURNED' }"
    :data-item-id="item.itemId"
  >
    <div class="head">
      <span class="step">
        <Icon v-if="filled" name="check" :size="26" />
        <template v-else>{{ index || '' }}</template>
      </span>
      <span class="name">{{ item.name }}</span>
      <span v-if="fileType === 'DOCUMENT'" class="type-chip">서류</span>
      <span class="status-chip" :class="filled ? (auto ? 'ai' : 'ok') : 'todo'">
        {{ filled ? (auto ? 'AI 배정' : '완료') : '필요' }}
      </span>
    </div>

    <p v-if="item.description" class="desc">{{ item.description }}</p>

    <div v-if="item.adminNote" class="admin-note">
      <strong>요청 사항</strong>
      <span>{{ item.adminNote }}</span>
    </div>

    <div v-if="item.status === 'RETURNED' && item.rejectReason" class="reject">
      <strong>반환 사유</strong>
      <span>{{ item.rejectReason }}</span>
    </div>

    <!-- 선택한 서류 -->
    <DocPreview v-if="stagedDoc" :doc="stagedDoc" />

    <!-- 예시 사진(위) → 내 사진(아래) -->
    <div v-if="allowsPhoto" class="images">
      <div v-if="item.exampleImageUrl" class="thumb" @click="emit('zoom', absoluteUrl(item.exampleImageUrl))">
        <span class="thumb-label">예시</span>
        <img :src="absoluteUrl(item.exampleImageUrl)" alt="예시" />
      </div>
      <div v-if="previewUrl" class="thumb" @click="emit('zoom', previewUrl)">
        <span class="thumb-label uploaded">내 사진</span>
        <img :src="previewUrl" alt="선택한 사진" />
      </div>
    </div>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 10px">{{ error }}</div>

    <div class="actions">
      <button
        v-if="filled && swappable"
        class="btn btn-ghost tap-btn swap-btn"
        @click="emit('swap', item.itemId)"
      >
        <Icon name="swap" :size="22" />
        <span>위치 바꾸기</span>
      </button>
      <button v-if="allowsPhoto" class="btn btn-ghost tap-btn" @click="pickCamera">
        <Icon name="camera" :size="22" />
        <span>{{ previewUrl ? '다시 촬영' : '카메라 촬영' }}</span>
      </button>
      <button v-if="allowsPhoto" class="btn btn-ghost tap-btn" @click="pickGallery">
        <Icon name="image" :size="22" />
        <span>사진첩</span>
      </button>
      <button v-if="allowsDoc" class="btn btn-ghost tap-btn" @click="pickDoc">
        <Icon name="fileText" :size="22" />
        <span>{{ stagedDoc ? '다른 파일' : '파일 선택' }}</span>
      </button>
    </div>

    <input
      ref="cameraInput"
      type="file"
      accept="image/*"
      capture="environment"
      hidden
      @change="onFileChange"
    />
    <input ref="galleryInput" type="file" accept="image/*" hidden @change="onFileChange" />
    <input ref="docInput" type="file" :accept="acceptFor('DOCUMENT')" hidden @change="onFileChange" />
  </div>
</template>

<style scoped>
/* design.md: 흰 카드 + 1px 연한 테두리, 채워지면 틴트로 전환 (그림자·색 없음) */
.item-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 18px;
  transition: background 0.15s, border-color 0.15s;
}
.item-card.done {
  background: var(--primary-soft);
  border-color: transparent;
}
.item-card.returned:not(.done) {
  border-color: var(--border-strong);
}
.head {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}
.step {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 30%;
  flex: 0 0 auto;
  font-size: 18px;
  font-weight: 652;
  color: var(--text);
  background: var(--primary-soft);
}
.item-card.done .step {
  color: #fff;
  background: var(--primary);
}
.name {
  font-size: 23px;
  font-weight: 652;
  line-height: 1.25;
  flex: 1;
  min-width: 0;
}
.status-chip,
.type-chip {
  font-size: 15px;
  font-weight: 600;
  padding: 5px 12px;
  border-radius: 9999px;
  flex: 0 0 auto;
}
.status-chip.ok {
  color: #fff;
  background: var(--primary);
}
.status-chip.todo {
  color: var(--text-muted);
  background: var(--primary-soft);
}
.item-card.done .status-chip.todo,
.item-card.done .type-chip {
  background: var(--card);
}
.status-chip.ai {
  color: var(--text);
  background: var(--card);
  box-shadow: inset 0 0 0 1px var(--border-strong);
}
.type-chip {
  color: var(--text);
  background: var(--primary-soft);
}
.desc {
  margin: 0 0 12px;
  font-size: 19px;
  line-height: 1.5;
  color: var(--text-muted);
}
.admin-note,
.reject {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--primary-soft);
  border-radius: var(--radius-sm);
  padding: 14px 16px;
  margin-bottom: 12px;
  font-size: 19px;
  line-height: 1.5;
}
.item-card.done .admin-note,
.item-card.done .reject {
  background: var(--card);
}
.admin-note strong,
.reject strong {
  color: var(--text);
  font-size: 16px;
  font-weight: 652;
}
.images {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 14px;
}
.thumb {
  position: relative;
  width: 100%;
  aspect-ratio: 1 / 1; /* 가로·세로 사진 모두 동일한 정사각형 박스에 표시 */
  border-radius: var(--radius-sm);
  background: var(--primary-soft);
  overflow: hidden;
  cursor: pointer;
}
.item-card.done .thumb {
  background: var(--card);
}
.thumb img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 잘리지 않고 전체가 보이도록 */
  display: block;
}
.thumb-label {
  position: absolute;
  top: 10px;
  left: 10px;
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  background: rgba(115, 115, 115, 0.56); /* design.md badge-overlay */
  padding: 4px 12px;
  border-radius: 9999px;
}
.thumb-label.uploaded {
  background: var(--primary);
}
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tap-btn {
  flex: 1;
  flex-direction: column;
  gap: 6px;
  padding: 14px 8px;
  font-size: 18px;
  border-radius: var(--radius-sm);
}
/* 위치 바꾸기: 한 줄 전체 차지 */
.swap-btn {
  flex: 1 1 100%;
  flex-direction: row;
  padding: 12px 8px;
  border-radius: 9999px;
}
</style>
