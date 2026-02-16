'use client';

import React, { useCallback, useEffect, useState } from 'react';
import {
  Autocomplete,
  Box,
  Button,
  Checkbox,
  Chip,
  Collapse,
  Container,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
  Alert,
  Tooltip,
} from '@mui/material';
import {
  CloudUpload as CloudUploadIcon,
  Refresh as RefreshIcon,
  Delete as DeleteIcon,
  KeyboardArrowDown as ExpandIcon,
  KeyboardArrowUp as CollapseIcon,
  Replay as RetryIcon,
  Check as CheckIcon,
  Undo as UndoIcon,
  CallMerge as MergeIcon,
  CheckCircle as CheckCircleIcon,
} from '@mui/icons-material';
import { expenseInputJobService, ExpenseInputJobDTO, TemporaryExpenseRecordDTO } from '@/services/expenseInputJobService';
import { categoryService } from '@/services/categoryService';
import { CategoryDTO } from '@/types/category';
import { BulkUploadDialog } from '@/components/expenses/BulkUploadDialog';

const statusColor = (status: ExpenseInputJobDTO['status']): 'default' | 'warning' | 'error' | 'info' | 'success' => {
  switch (status) {
    case 'UPLOADED':
      return 'default';
    case 'PROCESSING':
      return 'warning';
    case 'RETRYABLE':
      return 'warning';
    case 'FAILED':
      return 'error';
    case 'PROCESSED':
      return 'info';
    case 'COMPLETED':
      return 'success';
    default:
      return 'default';
  }
};

interface EditedFields {
  amount?: number;
  description?: string;
  expenseDate?: string;
  categoryId?: number | null;
}

const getCategoryLabel = (cat: CategoryDTO): string => {
  return cat.parentCategory ? `${cat.parentCategory.name} > ${cat.name}` : cat.name;
};

export default function BulkUploadExpensePage() {
  const [jobs, setJobs] = useState<ExpenseInputJobDTO[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [uploadDialogOpen, setUploadDialogOpen] = useState(false);
  const [expandedJobIds, setExpandedJobIds] = useState<Set<number>>(new Set());
  const [editedRecords, setEditedRecords] = useState<Map<number, EditedFields>>(new Map());
  const [categories, setCategories] = useState<CategoryDTO[]>([]);
  const [selectedRecordIds, setSelectedRecordIds] = useState<Map<number, Set<number>>>(new Map());

  const fetchJobs = useCallback(async () => {
    try {
      const data = await expenseInputJobService.getJobs();
      setJobs(data);
    } catch (err) {
      console.error('Failed to load jobs', err);
      setError('Failed to load jobs. Please try again.');
    }
  }, []);

  useEffect(() => {
    fetchJobs();
  }, [fetchJobs]);

  useEffect(() => {
    categoryService.getAllCategories().then(setCategories).catch(console.error);
  }, []);

  // Auto-poll when jobs are in UPLOADED or PROCESSING status
  useEffect(() => {
    const hasProcessing = jobs.some((job) => job.status === 'UPLOADED' || job.status === 'PROCESSING');
    if (!hasProcessing) return;
    const timer = setInterval(() => {
      fetchJobs();
    }, 3000);
    return () => clearInterval(timer);
  }, [jobs, fetchJobs]);

  const handleUpload = async (files: File[], categoryId: number | null) => {
    setLoading(true);
    setError('');
    try {
      await expenseInputJobService.uploadJobs(files, categoryId);
      await fetchJobs();
    } catch (err) {
      console.error('Failed to upload files', err);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteJob = async (jobId: number) => {
    if (!window.confirm('Are you sure you want to delete this job and all its records?')) return;
    setLoading(true);
    setError('');
    try {
      await expenseInputJobService.deleteJobs([jobId]);
      setExpandedJobIds((prev) => {
        const next = new Set(prev);
        next.delete(jobId);
        return next;
      });
      setSelectedRecordIds((prev) => {
        const next = new Map(prev);
        next.delete(jobId);
        return next;
      });
      await fetchJobs();
    } catch (err) {
      console.error('Failed to delete job', err);
      setError('Failed to delete job.');
    } finally {
      setLoading(false);
    }
  };

  const handleRetryJob = async (jobId: number) => {
    setLoading(true);
    setError('');
    try {
      await expenseInputJobService.retryJob(jobId);
      await fetchJobs();
    } catch (err) {
      console.error('Failed to retry job', err);
      setError('Failed to retry job.');
    } finally {
      setLoading(false);
    }
  };

  const toggleExpand = (jobId: number) => {
    setExpandedJobIds((prev) => {
      const next = new Set(prev);
      if (next.has(jobId)) {
        next.delete(jobId);
      } else {
        next.add(jobId);
      }
      return next;
    });
  };

  const handleEditField = (recordId: number, field: keyof EditedFields, value: EditedFields[keyof EditedFields]) => {
    setEditedRecords((prev) => {
      const next = new Map(prev);
      const existing = next.get(recordId) || {};
      next.set(recordId, { ...existing, [field]: value });
      return next;
    });
  };

  const handleSaveRecord = async (record: TemporaryExpenseRecordDTO) => {
    const edits = editedRecords.get(record.id) || {};
    const payload = {
      amount: edits.amount ?? record.amount,
      description: edits.description ?? record.description,
      expenseDate: edits.expenseDate ?? record.expenseDate,
      categoryId: edits.categoryId !== undefined ? edits.categoryId : (record.categoryId ?? null),
    };
    try {
      await expenseInputJobService.updateTemporaryRecord(record.id, payload);
      setEditedRecords((prev) => {
        const next = new Map(prev);
        next.delete(record.id);
        return next;
      });
      await fetchJobs();
    } catch (err) {
      console.error('Failed to save record', err);
      setError('Failed to save record.');
    }
  };

  const handleUndoRecord = (recordId: number) => {
    setEditedRecords((prev) => {
      const next = new Map(prev);
      next.delete(recordId);
      return next;
    });
  };

  const toggleRecordSelect = (jobId: number, recordId: number) => {
    setSelectedRecordIds((prev) => {
      const next = new Map(prev);
      const jobSet = new Set(next.get(jobId) || []);
      if (jobSet.has(recordId)) {
        jobSet.delete(recordId);
      } else {
        jobSet.add(recordId);
      }
      if (jobSet.size === 0) {
        next.delete(jobId);
      } else {
        next.set(jobId, jobSet);
      }
      return next;
    });
  };

  const toggleSelectAllRecords = (jobId: number, records: TemporaryExpenseRecordDTO[]) => {
    setSelectedRecordIds((prev) => {
      const next = new Map(prev);
      const jobSet = next.get(jobId) || new Set();
      if (jobSet.size === records.length) {
        next.delete(jobId);
      } else {
        next.set(jobId, new Set(records.map((r) => r.id)));
      }
      return next;
    });
  };

  const handleMergeRecords = async (jobId: number, records: TemporaryExpenseRecordDTO[]) => {
    const selected = selectedRecordIds.get(jobId);
    if (!selected || selected.size < 2) return;

    setLoading(true);
    setError('');
    try {
      // Save any pending edits for selected records first
      for (const recordId of selected) {
        if (editedRecords.has(recordId)) {
          const record = records.find((r) => r.id === recordId);
          if (record) {
            await handleSaveRecord(record);
          }
        }
      }
      await expenseInputJobService.mergeRecords(Array.from(selected));
      setSelectedRecordIds((prev) => {
        const next = new Map(prev);
        next.delete(jobId);
        return next;
      });
      await fetchJobs();
    } catch (err) {
      console.error('Failed to merge records', err);
      setError('Failed to merge records.');
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteRecords = async (jobId: number) => {
    const selected = selectedRecordIds.get(jobId);
    if (!selected || selected.size === 0) return;

    setLoading(true);
    setError('');
    try {
      await expenseInputJobService.deleteRecords(Array.from(selected));
      setSelectedRecordIds((prev) => {
        const next = new Map(prev);
        next.delete(jobId);
        return next;
      });
      // Clear edited state for deleted records
      setEditedRecords((prev) => {
        const next = new Map(prev);
        for (const recordId of selected) {
          next.delete(recordId);
        }
        return next;
      });
      await fetchJobs();
    } catch (err) {
      console.error('Failed to delete records', err);
      setError('Failed to delete records.');
    } finally {
      setLoading(false);
    }
  };

  const handleCompleteJob = async (jobId: number) => {
    setLoading(true);
    setError('');
    try {
      await expenseInputJobService.completeJob(jobId);
      await fetchJobs();
    } catch (err: any) {
      console.error('Failed to complete job', err);
      const msg = err?.response?.data?.message || err?.response?.data || 'Failed to complete job.';
      setError(typeof msg === 'string' ? msg : 'Failed to complete job.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5">Bulk Upload</Typography>
          <Typography variant="body2" color="text.secondary">
            Upload receipts or screenshots to extract expense records.
          </Typography>
        </Box>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button
            variant="contained"
            startIcon={<CloudUploadIcon />}
            disabled={loading}
            onClick={() => setUploadDialogOpen(true)}
          >
            Bulk Upload
          </Button>
          <Button
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={fetchJobs}
            disabled={loading}
          >
            Refresh
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Status</TableCell>
              <TableCell>Filename</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Message</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {jobs.map((job) => {
              const canExpand = job.status === 'PROCESSED' || job.status === 'COMPLETED';
              const isExpanded = expandedJobIds.has(job.id);
              const isReadOnly = job.status === 'COMPLETED';
              const records = job.temporaryRecords || [];
              const jobSelected = selectedRecordIds.get(job.id) || new Set<number>();
              const allSelected = records.length > 0 && jobSelected.size === records.length;

              return (
                <React.Fragment key={job.id}>
                  <TableRow hover>
                    <TableCell>
                      <Chip label={job.status} color={statusColor(job.status)} size="small" />
                    </TableCell>
                    <TableCell>{job.originalFilename}</TableCell>
                    <TableCell>{new Date(job.createdAt).toLocaleString()}</TableCell>
                    <TableCell>
                      {job.errorMessage && (
                        <Typography variant="caption" color="error">
                          {job.errorMessage}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell align="right">
                      {job.status === 'PROCESSED' && (
                        <Tooltip title="Complete job">
                          <IconButton size="small" color="success" onClick={() => handleCompleteJob(job.id)} disabled={loading}>
                            <CheckCircleIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      {canExpand && (
                        <Tooltip title={isExpanded ? 'Collapse' : 'Expand records'}>
                          <IconButton size="small" onClick={() => toggleExpand(job.id)}>
                            {isExpanded ? <CollapseIcon fontSize="small" /> : <ExpandIcon fontSize="small" />}
                          </IconButton>
                        </Tooltip>
                      )}
                      {job.status === 'RETRYABLE' && (
                        <Tooltip title="Retry processing">
                          <IconButton size="small" onClick={() => handleRetryJob(job.id)} disabled={loading}>
                            <RetryIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      <Tooltip title="Delete job">
                        <IconButton size="small" onClick={() => handleDeleteJob(job.id)} disabled={loading}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  </TableRow>
                  {canExpand && (
                    <TableRow>
                      <TableCell colSpan={5} sx={{ p: 0, borderBottom: isExpanded ? undefined : 0 }}>
                        <Collapse in={isExpanded} unmountOnExit>
                          <Box sx={{ p: 2, bgcolor: 'action.hover' }}>
                            {!isReadOnly && records.length > 0 && (
                              <Box sx={{ display: 'flex', gap: 1, mb: 1 }}>
                                <Button
                                  size="small"
                                  variant="outlined"
                                  startIcon={<MergeIcon />}
                                  disabled={loading || jobSelected.size < 2}
                                  onClick={() => handleMergeRecords(job.id, records)}
                                >
                                  Merge
                                </Button>
                                <Button
                                  size="small"
                                  variant="outlined"
                                  color="error"
                                  startIcon={<DeleteIcon />}
                                  disabled={loading || jobSelected.size === 0}
                                  onClick={() => handleDeleteRecords(job.id)}
                                >
                                  Delete
                                </Button>
                              </Box>
                            )}
                            <Table size="small">
                              <TableHead>
                                <TableRow>
                                  {!isReadOnly && (
                                    <TableCell padding="checkbox">
                                      <Checkbox
                                        size="small"
                                        checked={allSelected}
                                        indeterminate={jobSelected.size > 0 && !allSelected}
                                        onChange={() => toggleSelectAllRecords(job.id, records)}
                                      />
                                    </TableCell>
                                  )}
                                  <TableCell>Date</TableCell>
                                  <TableCell>Description</TableCell>
                                  <TableCell>Amount</TableCell>
                                  <TableCell>Category</TableCell>
                                  <TableCell align="right">Actions</TableCell>
                                </TableRow>
                              </TableHead>
                              <TableBody>
                                {records.map((record) => {
                                  const edits = editedRecords.get(record.id) || {};
                                  const hasEdits = editedRecords.has(record.id);

                                  return (
                                    <TableRow key={record.id}>
                                      {!isReadOnly && (
                                        <TableCell padding="checkbox">
                                          <Checkbox
                                            size="small"
                                            checked={jobSelected.has(record.id)}
                                            onChange={() => toggleRecordSelect(job.id, record.id)}
                                          />
                                        </TableCell>
                                      )}
                                      <TableCell sx={{ minWidth: 140 }}>
                                        <TextField
                                          type="date"
                                          size="small"
                                          variant="standard"
                                          value={edits.expenseDate ?? record.expenseDate}
                                          onChange={(e) => handleEditField(record.id, 'expenseDate', e.target.value)}
                                          disabled={isReadOnly}
                                          fullWidth
                                        />
                                      </TableCell>
                                      <TableCell>
                                        <TextField
                                          size="small"
                                          variant="standard"
                                          value={edits.description ?? record.description}
                                          onChange={(e) => handleEditField(record.id, 'description', e.target.value)}
                                          disabled={isReadOnly}
                                          fullWidth
                                          multiline
                                        />
                                      </TableCell>
                                      <TableCell sx={{ minWidth: 100 }}>
                                        <TextField
                                          type="number"
                                          size="small"
                                          variant="standard"
                                          value={edits.amount ?? record.amount}
                                          onChange={(e) => handleEditField(record.id, 'amount', parseFloat(e.target.value) || 0)}
                                          disabled={isReadOnly}
                                          fullWidth
                                          inputProps={{ step: '0.01', min: '0' }}
                                        />
                                      </TableCell>
                                      <TableCell sx={{ minWidth: 200 }}>
                                        <Autocomplete
                                          size="small"
                                          options={categories}
                                          value={categories.find((c) => c.id === (edits.categoryId !== undefined ? edits.categoryId : record.categoryId)) || null}
                                          onChange={(_, newValue) => handleEditField(record.id, 'categoryId', newValue?.id ?? null)}
                                          getOptionLabel={getCategoryLabel}
                                          disabled={isReadOnly}
                                          renderInput={(params) => (
                                            <TextField {...params} variant="standard" placeholder="Category" />
                                          )}
                                        />
                                      </TableCell>
                                      <TableCell align="right" sx={{ whiteSpace: 'nowrap' }}>
                                        {!isReadOnly && (
                                          <>
                                            <Tooltip title="Save changes">
                                              <IconButton size="small" onClick={() => handleSaveRecord(record)} disabled={loading}>
                                                <CheckIcon fontSize="small" />
                                              </IconButton>
                                            </Tooltip>
                                            {hasEdits && (
                                              <Tooltip title="Undo changes">
                                                <IconButton size="small" onClick={() => handleUndoRecord(record.id)}>
                                                  <UndoIcon fontSize="small" />
                                                </IconButton>
                                              </Tooltip>
                                            )}
                                          </>
                                        )}
                                      </TableCell>
                                    </TableRow>
                                  );
                                })}
                                {records.length === 0 && (
                                  <TableRow>
                                    <TableCell colSpan={isReadOnly ? 6 : 7} align="center">
                                      <Typography variant="body2" color="text.secondary">
                                        No records extracted.
                                      </Typography>
                                    </TableCell>
                                  </TableRow>
                                )}
                              </TableBody>
                            </Table>
                          </Box>
                        </Collapse>
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
              );
            })}
            {jobs.length === 0 && (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    No jobs yet. Upload files to get started.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <BulkUploadDialog
        open={uploadDialogOpen}
        onClose={() => setUploadDialogOpen(false)}
        onUpload={handleUpload}
      />
    </Container>
  );
}
