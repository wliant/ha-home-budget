import api from './api';

export interface TemporaryExpenseRecordDTO {
  id: number;
  amount: number;
  description: string;
  expenseDate: string;
  categoryId?: number | null;
  categoryName?: string;
  categoryIcon?: string;
  confirmed: boolean;
  confirmedAt?: string | null;
}

export interface ExpenseInputJobDTO {
  id: number;
  status: 'UPLOADED' | 'PROCESSING' | 'RETRYABLE' | 'FAILED' | 'PROCESSED' | 'COMPLETED';
  originalFilename: string;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
  temporaryRecords?: TemporaryExpenseRecordDTO[] | null;
}

export interface UpdateTemporaryExpenseRecordRequest {
  amount: number;
  description: string;
  expenseDate: string;
  categoryId?: number | null;
}

export const expenseInputJobService = {
  uploadJobs: async (files: File[]): Promise<ExpenseInputJobDTO[]> => {
    const formData = new FormData();
    files.forEach((file) => formData.append('files', file));
    const response = await api.post<ExpenseInputJobDTO[]>('/api/expense-input-jobs', formData);
    return response.data;
  },

  getJobs: async (): Promise<ExpenseInputJobDTO[]> => {
    const response = await api.get<ExpenseInputJobDTO[]>('/api/expense-input-jobs');
    return response.data;
  },

  updateTemporaryRecord: async (
    recordId: number,
    request: UpdateTemporaryExpenseRecordRequest
  ): Promise<TemporaryExpenseRecordDTO> => {
    const response = await api.patch<TemporaryExpenseRecordDTO>(
      `/api/expense-input-jobs/temporary-records/${recordId}`,
      request
    );
    return response.data;
  },

  deleteJobs: async (jobIds: number[]): Promise<void> => {
    await api.delete('/api/expense-input-jobs', { data: { jobIds } });
  },

  retryJob: async (jobId: number): Promise<ExpenseInputJobDTO> => {
    const response = await api.post<ExpenseInputJobDTO>(`/api/expense-input-jobs/${jobId}/retry`);
    return response.data;
  },

  mergeRecords: async (recordIds: number[]): Promise<TemporaryExpenseRecordDTO> => {
    const response = await api.post<TemporaryExpenseRecordDTO>(
      '/api/expense-input-jobs/temporary-records/merge',
      { recordIds }
    );
    return response.data;
  },

  deleteRecords: async (recordIds: number[]): Promise<void> => {
    await api.delete('/api/expense-input-jobs/temporary-records', { data: { recordIds } });
  },

  completeJob: async (jobId: number): Promise<ExpenseInputJobDTO> => {
    const response = await api.post<ExpenseInputJobDTO>(`/api/expense-input-jobs/${jobId}/complete`);
    return response.data;
  },
};
