import { getAuthHeader } from "../composables/useAuth";
import type { Document } from "../types/document";

export type CreateDocumentDTO = {
  content: string;
};

export type UpdateDocumentDTO = {
  content: string;
};

export const fetchDocuments = async (): Promise<Document[]> => {
  try {
    const response = await fetch('/api/documents', {
        method: "GET",
        headers: { 
            "Content-Type": "application/json", 
            ...getAuthHeader() 
        }
    });

    if (!response.ok) {
      let errMsg = response.statusText;
      try {
        const errData = await response.json();
        if (errData && (errData.message || errData.error)) {
          errMsg = errData.message || errData.error;
        } else if (errData) {
          errMsg = JSON.stringify(errData);
        }
      } catch {}

      throw new Error(errMsg);
    }

    return response.json() as Promise<Document[]>;

  } catch (error) {
    throw new Error(error instanceof Error ? error.message : 'Failed to fetch documents');
  }
}

export const createDocument = async (document: CreateDocumentDTO): Promise<Document> => {
  try {
    const response = await fetch('/api/documents', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(document),
    });

    if (!response.ok) {
      let errMsg = response.statusText;
      try {
        const errData = await response.json();
        if (errData && (errData.message || errData.error)) {
          errMsg = errData.message || errData.error;
        } else if (errData) {
          errMsg = JSON.stringify(errData);
        }
      } catch {}

      throw new Error(errMsg);
    }

    return response.json() as Promise<Document>;
  } catch (error) {
    throw new Error(error instanceof Error ? error.message : 'Failed to create document');
  }
}

export const updateDocument = async (
  documentId: number,
  document: UpdateDocumentDTO,
): Promise<Document> => {
  try {
    const response = await fetch(`/api/documents/${documentId}`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(document),
    });

    if (!response.ok) {
      let errMsg = response.statusText;
      try {
        const errData = await response.json();
        if (errData && (errData.message || errData.error)) {
          errMsg = errData.message || errData.error;
        } else if (errData) {
          errMsg = JSON.stringify(errData);
        }
      } catch {}

      throw new Error(errMsg);
    }

    return response.json() as Promise<Document>;
  } catch (error) {
    throw new Error(error instanceof Error ? error.message : 'Failed to update document');
  }
}

export const deleteDocument = async (documentId: number): Promise<void> => {
  try {
    const response = await fetch(`/api/documents/${documentId}`, {
      method: 'DELETE',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    });

    if (!response.ok) {
      let errMsg = response.statusText;
      try {
        const errData = await response.json();
        if (errData && (errData.message || errData.error)) {
          errMsg = errData.message || errData.error;
        } else if (errData) {
          errMsg = JSON.stringify(errData);
        }
      } catch {}

      throw new Error(errMsg);
    }
  } catch (error) {
    throw new Error(error instanceof Error ? error.message : 'Failed to delete document');
  }
}
