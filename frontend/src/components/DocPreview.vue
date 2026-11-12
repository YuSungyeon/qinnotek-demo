<script setup>
import { computed } from 'vue'
import { fileUrl } from '../api/client'
import { formatBytes } from '../lib/filekind'
import Icon from './Icon.vue'

const props = defineProps({
  /** 서버가 돌려준 스테이징 문서 { stagedId, originalFileName, sizeBytes, conversionStatus, failureMessage, pdfUrl } */
  doc: { type: Object, required: true }
})

const statusChip = computed(() => {
  if (props.doc.conversionStatus === 'PENDING') return { text: '분석 중', tone: 'wait' }
  if (props.doc.conversionStatus === 'FAILED') return { text: '원본 그대로 제출', tone: 'warn' }
  return { text: 'PDF 변환 완료', tone: 'ok' }
})

// 모바일 Safari 의 iframe PDF 는 첫 페이지만 렌더링되므로 새 탭으로 연다
function openPdf() {
  window.open(fileUrl(props.doc.pdfUrl), '_blank', 'noopener')
}
</script>

<template>
  <div class="doc-preview">
    <span class="doc-icon"><Icon name="fileText" :size="30" /></span>
    <div class="doc-info">
      <b class="doc-name">{{ doc.originalFileName }}</b>
      <small class="doc-meta">
        {{ formatBytes(doc.sizeBytes) }}
        <span class="chip" :class="statusChip.tone">{{ statusChip.text }}</span>
      </small>
      <small v-if="doc.conversionStatus === 'FAILED'" class="doc-warn">
        {{ doc.failureMessage || 'PDF 사본을 만들지 못했어요.' }} 원본 파일은 그대로 제출됩니다.
      </small>
    </div>
    <button v-if="doc.pdfUrl" class="btn btn-ghost btn-sm" @click="openPdf">PDF 보기</button>
  </div>
</template>

<style scoped>
.doc-preview {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--card);
  margin-bottom: 14px;
}
.doc-icon {
  width: 52px;
  height: 52px;
  flex: 0 0 auto;
  border-radius: 30%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--text);
  background: var(--primary-soft);
}
.doc-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.doc-name {
  font-size: 20px;
  font-weight: 652;
  word-break: break-all;
}
.doc-meta {
  font-size: 16px;
  color: var(--text-muted);
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 8px;
}
.doc-warn {
  font-size: 16px;
  color: var(--danger);
  line-height: 1.4;
}
.chip {
  font-size: 15px;
  font-weight: 700;
  padding: 2px 10px;
  border-radius: 999px;
  white-space: nowrap;
}
.doc-preview .btn {
  flex: 0 0 auto;
  white-space: nowrap;
}
.chip.ok {
  color: #fff;
  background: var(--primary);
}
.chip.wait {
  color: var(--primary-dark);
  background: var(--primary-soft);
}
.chip.warn {
  color: var(--text);
  background: var(--card);
  box-shadow: inset 0 0 0 1px var(--border-strong);
}
</style>
