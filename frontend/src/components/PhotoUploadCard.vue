<script setup>
import { ref } from 'vue'
import { customerApi } from '../api/customer'
import { fileUrl } from '../api/client'

const props = defineProps({
  item: { type: Object, required: true }
})
const emit = defineEmits(['uploaded'])

const cameraInput = ref(null)
const galleryInput = ref(null)
const uploading = ref(false)
const error = ref('')

function pickCamera() {
  cameraInput.value?.click()
}
function pickGallery() {
  galleryInput.value?.click()
}

async function onFileChange(e) {
  const file = e.target.files?.[0]
  e.target.value = '' // 같은 파일 재선택 허용
  if (!file) return
  error.value = ''
  uploading.value = true
  try {
    const updated = await customerApi.uploadPhoto(props.item.itemId, file)
    emit('uploaded', updated)
  } catch (err) {
    error.value = err.message
  } finally {
    uploading.value = false
  }
}
</script>

<template>
  <div class="photo-card" :class="{ returned: item.status === 'RETURNED' }" :data-item-id="item.itemId">
    <div class="head">
      <span class="name">{{ item.name }}</span>
      <span v-if="item.hasPhoto" class="done-chip">✓ 업로드됨</span>
      <span v-else class="todo-chip">미업로드</span>
    </div>

    <p v-if="item.description" class="desc">{{ item.description }}</p>

    <div v-if="item.status === 'RETURNED' && item.rejectReason" class="reject">
      <strong>반환 사유</strong>
      <span>{{ item.rejectReason }}</span>
    </div>

    <div class="images">
      <div v-if="item.exampleImageUrl" class="thumb">
        <span class="thumb-label">예시</span>
        <img :src="fileUrl(item.exampleImageUrl)" alt="예시" />
      </div>
      <div v-if="item.hasPhoto" class="thumb">
        <span class="thumb-label uploaded">내 사진</span>
        <img :src="fileUrl(item.photoUrl)" alt="업로드한 사진" />
      </div>
    </div>

    <div v-if="error" class="alert alert-error" style="margin-bottom: 10px">{{ error }}</div>

    <div class="actions">
      <button class="btn btn-ghost btn-sm" :disabled="uploading" @click="pickCamera">📷 카메라 촬영</button>
      <button class="btn btn-ghost btn-sm" :disabled="uploading" @click="pickGallery">🖼️ 사진첩</button>
      <span v-if="uploading" class="uploading-text">업로드 중…</span>
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
  </div>
</template>

<style scoped>
.photo-card {
  background: var(--card);
  border: 1px solid var(--border);
  border-radius: var(--radius);
  padding: 16px;
}
.photo-card.returned {
  border-color: #fca5a5;
  background: #fffafa;
}
.head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 6px;
}
.name {
  font-size: 24px;
  font-weight: 700;
}
.done-chip {
  font-size: 18px;
  font-weight: 700;
  color: var(--success);
}
.todo-chip {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-muted);
}
.desc {
  margin: 0 0 10px;
  font-size: 21px;
  color: var(--text-muted);
}
.reject {
  display: flex;
  flex-direction: column;
  gap: 2px;
  background: var(--danger-bg);
  border: 1px solid #fecaca;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 12px;
  font-size: 21px;
}
.reject strong {
  color: var(--danger);
  font-size: 18px;
}
.images {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}
.thumb {
  position: relative;
  flex: 1 1 0;
  min-width: 0;
}
.thumb img {
  width: 100%;
  height: auto;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid var(--border);
  display: block;
}
.thumb-label {
  position: absolute;
  top: 6px;
  left: 6px;
  font-size: 15px;
  font-weight: 700;
  color: #fff;
  background: rgba(0, 0, 0, 0.55);
  padding: 2px 9px;
  border-radius: 999px;
}
.thumb-label.uploaded {
  background: var(--success);
}
.actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.actions .btn-sm {
  font-size: 20px;
  padding: 10px 16px;
}
.uploading-text {
  font-size: 20px;
  color: var(--primary);
}
</style>
