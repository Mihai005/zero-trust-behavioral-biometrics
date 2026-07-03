<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useBiometricStream } from "../composables/useBiometricStream";
import { useAuth } from "../composables/useAuth";
import { createDocument, fetchDocuments, updateDocument } from "../service/documentApi";
import type { KeystrokeData } from "../types/types";

const { token } = useAuth();
const router = useRouter();
const route = useRoute();
const { trustScore, sendKeystrokeBatch } = useBiometricStream(token as any);

const text = ref<string>("");
const isSaving = ref(false);
const isLoadingDocument = ref(false);
const activeDocumentId = ref<number | null>(null);
const notification = ref<{ show: boolean; message: string; color: string }>({
  show: false,
  message: "",
  color: "success",
});
const actionLabel = computed(() => (activeDocumentId.value ? "Update" : "Save"));
let currentBatch: KeystrokeData[] = [];
const BATCH_SIZE = 40;

const notify = (message: string, color = "success") => {
  notification.value = {
    show: true,
    message,
    color,
  };
};

const parseDocumentId = (value: unknown): number | null => {
  const raw = Array.isArray(value) ? value[0] : value;
  const parsed = Number(raw);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : null;
};

const loadDocumentFromQuery = async () => {
  const requestedId = parseDocumentId(route.query.documentId);
  activeDocumentId.value = requestedId;

  if (!requestedId) {
    text.value = "";
    return;
  }

  isLoadingDocument.value = true;

  try {
    const documents = await fetchDocuments();
    const selected = documents.find((document) => document.id === requestedId);

    if (!selected) {
      notify("Selected document was not found.", "error");
      return;
    }

    text.value = selected.content ?? "";
  } catch (error) {
    notify(
      error instanceof Error ? error.message : "Failed to load selected document",
      "error",
    );
  } finally {
    isLoadingDocument.value = false;
  }
};

const saveDocument = async () => {
  const content = text.value.trim();

  if (!content) {
    notify("Write something before saving.", "error");
    return;
  }

  isSaving.value = true;

  try {
    if (activeDocumentId.value) {
      await updateDocument(activeDocumentId.value, { content: content });
      notify("Document updated successfully.");
    } else {
      await createDocument({ content });
      text.value = "";
      notify("Document saved successfully.");
    }

    await router.push("/dashboard");
  } catch (error) {
    notify(
      error instanceof Error
        ? error.message
        : activeDocumentId.value
          ? "Failed to update document"
          : "Failed to save document",
      "error",
    );
  } finally {
    isSaving.value = false;
  }
};

const handleKey = (e: KeyboardEvent, type: "keydown" | "keyup") => {
  if (e.repeat) return;

  currentBatch.push({
    key: e.code,
    timestamp: performance.now(),
    type: type,
  });

  if (currentBatch.length >= BATCH_SIZE) {
    sendKeystrokeBatch([...currentBatch]);
    currentBatch = [];
  }
};

onMounted(loadDocumentFromQuery);

watch(
  () => route.query.documentId,
  () => {
    loadDocumentFromQuery();
  },
);
</script>

<template>
  <v-container
    fluid
    class="pa-4 pa-sm-6 d-flex flex-column"
    style="min-height: 100dvh"
  >
    <v-card class="d-flex flex-column flex-grow-1" elevation="2" rounded="lg">
      <div
        class="d-flex flex-column flex-sm-row justify-space-between align-start align-sm-center pa-4 pa-sm-6"
        style="gap: 16px"
      >
        <div class="d-flex flex-column align-start text-left">
          <div class="text-overline text-primary mb-1">Secure Workspace</div>
          <h2 class="text-h4 font-weight-bold mb-2">Confidential Document</h2>
          <v-chip
            :color="trustScore === null ? 'warning' : (trustScore < 70 ? 'error' : 'success')"
            variant="tonal"
            label
            size="large"
          >
            <template v-if="trustScore === null">Profiling…</template>
            <template v-else>Trust Score: {{ trustScore }}%</template>
          </v-chip>
        </div>

        <div class="d-flex align-center flex-wrap" style="gap: 12px">
          <v-btn
            color="primary"
            variant="flat"
            :prepend-icon="activeDocumentId ? 'mdi-content-save-edit' : 'mdi-content-save'"
            size="large"
            :loading="isSaving"
            :disabled="isSaving || trustScore === null || trustScore < 70"
            @click="saveDocument"
          >
            {{ actionLabel }}
          </v-btn>
        </div>
      </div>

      <v-divider />

      <v-card-text
        class="d-flex flex-column flex-grow-1 pa-4 pa-sm-6 bg-surface"
      >
        <v-sheet
          class="mb-4 align-self-start secure-status-box"
          :color="trustScore === null ? 'warning' : (trustScore < 70 ? 'error' : 'success')"
          rounded="md"
          border
        >
          <v-icon
            :icon="trustScore === null ? 'mdi-progress-clock' : (trustScore < 70 ? 'mdi-alert-circle-outline' : 'mdi-check-circle-outline')"
            size="18"
            class="secure-status-box__icon"
          />
          <span class="text-body-2 text-no-wrap secure-status-box__text">
            <template v-if="trustScore === null">Profiling: typing will be used to build your biometric baseline.</template>
            <template v-else-if="trustScore < 70">Editing locked: Editor disabled.</template>
            <template v-else>Session trusted: You can edit normally.</template>
          </span>
        </v-sheet>

        <v-textarea
          v-model="text"
          class="textarea-stretch flex-grow-1"
          label="Document content"
          placeholder="Start typing here..."
          variant="outlined"
          :disabled="(trustScore !== null && trustScore < 70) || isLoadingDocument"
          :loading="isLoadingDocument"
          hide-details="auto"
          @keydown="handleKey($event, 'keydown')"
          @keyup="handleKey($event, 'keyup')"
        />
      </v-card-text>
    </v-card>

    <v-snackbar
      v-model="notification.show"
      :color="notification.color"
      location="top"
      timeout="3000"
      rounded="pill"
    >
      {{ notification.message }}
    </v-snackbar>
  </v-container>
</template>

<style scoped>
.textarea-stretch :deep(.v-input__control),
.textarea-stretch :deep(.v-field) {
  height: 100%;
}

.textarea-stretch :deep(.v-field__field) {
  height: 100%;
}

.secure-status-box {
  display: inline-flex;
  align-items: center;
  width: auto;
  min-width: 0;
  padding: 4px 10px;
}

.secure-status-box__icon {
  margin-inline-end: 6px;
}

.secure-status-box__text {
  line-height: 1.2;
}
</style>
