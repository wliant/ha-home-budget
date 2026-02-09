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
  status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
  originalFilename: string;
  errorMessage?: string | null;
  createdAt: string;
  updatedAt: string;
  temporaryRecord?: TemporaryExpenseRecordDTO | null;
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
    jobId: number,
    request: UpdateTemporaryExpenseRecordRequest
  ): Promise<TemporaryExpenseRecordDTO> => {
    const response = await api.patch<TemporaryExpenseRecordDTO>(
      `/api/expense-input-jobs/${jobId}/temporary-record`,
      request
    );
    return response.data;
  },

  confirmJobs: async (jobIds: number[]): Promise<number[]> => {
    const response = await api.post<number[]>('/api/expense-input-jobs/confirm', { jobIds });
    return response.data;
  },

  deleteJobs: async (jobIds: number[]): Promise<void> => {
    await api.delete('/api/expense-input-jobs', { data: { jobIds } });
  },
};
