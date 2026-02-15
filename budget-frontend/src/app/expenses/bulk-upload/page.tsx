'use client';

import { useEffect, useMemo, useState } from 'react';
import {
  Box,
  Button,
  Checkbox,
  Chip,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
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
  CheckCircle as CheckCircleIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
} from '@mui/icons-material';
import { expenseInputJobService, ExpenseInputJobDTO, TemporaryExpenseRecordDTO } from '@/services/expenseInputJobService';
import { CategorySelect } from '@/components/expenses/CategorySelect';
import { formatExpenseAmount } from '@/services/expenseService';

interface FlatRow {
  job: ExpenseInputJobDTO;
  record: TemporaryExpenseRecordDTO | null;
  isFirstRecord: boolean;
  recordCount: number;
}

const statusColor = (status: ExpenseInputJobDTO['status']) => {
  switch (status) {
    case 'COMPLETED':
      return 'success';
    case 'FAILED':
      return 'error';
    case 'PROCESSING':
      return 'warning';
    default:
      return 'default';
  }
};

export default function BulkUploadExpensePage() {
  const [jobs, setJobs] = useState<ExpenseInputJobDTO[]>([]);
  const [selected, setSelected] = useState<number[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [editingRecord, setEditingRecord] = useState<{ record: TemporaryExpenseRecordDTO; job: ExpenseInputJobDTO } | null>(null);
  const [editForm, setEditForm] = useState({
    amount: '',
    description: '',
    expenseDate: '',
    categoryId: null as number | null,
  });

  const fetchJobs = async () => {
    try {
      const data = await expenseInputJobService.getJobs();
      setJobs(data);
    } catch (err) {
      console.error('Failed to load jobs', err);
      setError('Failed to load jobs. Please try again.');
    }
  };

  useEffect(() => {
    fetchJobs();
  }, []);

  useEffect(() => {
    const hasProcessing = jobs.some((job) => job.status === 'PROCESSING' || job.status === 'PENDING');
    if (!hasProcessing) return;
    const timer = setInterval(() => {
      fetchJobs();
    }, 3000);
    return () => clearInterval(timer);
  }, [jobs]);

  const flatRows: FlatRow[] = useMemo(() => {
    const rows: FlatRow[] = [];
    for (const job of jobs) {
      const records = job.temporaryRecords ?? [];
      if (records.length === 0) {
        rows.push({ job, record: null, isFirstRecord: true, recordCount: 0 });
      } else {
        records.forEach((record, idx) => {
          rows.push({ job, record, isFirstRecord: idx === 0, recordCount: records.length });
        });
      }
    }
    return rows;
  }, [jobs]);

  const selectableJobIds = useMemo(
    () => {
      const ids = new Set<number>();
      for (const job of jobs) {
        const records = job.temporaryRecords ?? [];
        if (job.status === 'COMPLETED' && records.length > 0 && records.some((r) => !r.confirmed)) {
          ids.add(job.id);
        }
      }
      return Array.from(ids);
    },
    [jobs]
  );

  const toggleSelect = (jobId: number) => {
    setSelected((prev) =>
      prev.includes(jobId) ? prev.filter((id) => id !== jobId) : [...prev, jobId]
    );
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelected(selectableJobIds);
    } else {
      setSelected([]);
    }
  };

  const handleUpload = async (event: React.ChangeEvent<HTMLInputElement>) => {
    const files = Array.from(event.target.files || []);
    if (files.length === 0) return;
    setLoading(true);
    setError('');
    try {
      await expenseInputJobService.uploadJobs(files);
      await fetchJobs();
    } catch (err) {
      console.error('Failed to upload files', err);
      setError('Failed to upload files. Please try again.');
    } finally {
      setLoading(false);
      event.target.value = '';
    }
  };

  const handleConfirm = async () => {
    if (selected.length === 0) return;
    setLoading(true);
    try {
      await expenseInputJobService.confirmJobs(selected);
      setSelected([]);
      await fetchJobs();
    } catch (err) {
      console.error('Failed to confirm jobs', err);
      setError('Failed to confirm selected records.');
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (selected.length === 0) return;
    setLoading(true);
    try {
      await expenseInputJobService.deleteJobs(selected);
      setSelected([]);
      await fetchJobs();
    } catch (err) {
      console.error('Failed to delete jobs', err);
      setError('Failed to delete selected rows.');
    } finally {
      setLoading(false);
    }
  };

  const openEditDialog = (record: TemporaryExpenseRecordDTO, job: ExpenseInputJobDTO) => {
    if (record.confirmed) return;
    setEditingRecord({ record, job });
    setEditForm({
      amount: record.amount.toString(),
      description: record.description,
      expenseDate: record.expenseDate,
      categoryId: record.categoryId ?? null,
    });
  };

  const handleEditSave = async () => {
    if (!editingRecord) return;
    setLoading(true);
    try {
      await expenseInputJobService.updateTemporaryRecord(editingRecord.record.id, {
        amount: parseFloat(editForm.amount),
        description: editForm.description.trim(),
        expenseDate: editForm.expenseDate,
        categoryId: editForm.categoryId,
      });
      setEditingRecord(null);
      await fetchJobs();
    } catch (err) {
      console.error('Failed to update temporary record', err);
      setError('Failed to update record.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <Container maxWidth="lg" sx={{ py: 4 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3, flexWrap: 'wrap', gap: 2 }}>
        <Box>
          <Typography variant="h5">Bulk Upload Image Process</Typography>
          <Typography variant="body2" color="text.secondary">
            Upload receipts or screenshots and confirm generated expense records.
          </Typography>
        </Box>
        <Box>
          <Button
            variant="contained"
            component="label"
            startIcon={<CloudUploadIcon />}
            disabled={loading}
          >
            Bulk Upload
            <input hidden type="file" multiple accept="application/pdf,image/*" onChange={handleUpload} />
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Box sx={{ display: 'flex', gap: 2, mb: 2, flexWrap: 'wrap' }}>
        <Button
          variant="outlined"
          startIcon={<CheckCircleIcon />}
          disabled={loading || selected.length === 0}
          onClick={handleConfirm}
        >
          Confirm Selected
        </Button>
        <Button
          variant="outlined"
          color="error"
          startIcon={<DeleteIcon />}
          disabled={loading || selected.length === 0}
          onClick={handleDelete}
        >
          Delete Selected
        </Button>
      </Box>

      <TableContainer component={Paper}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell padding="checkbox">
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={selected.length > 0 && selected.length === selectableJobIds.length}
                      indeterminate={selected.length > 0 && selected.length < selectableJobIds.length}
                      onChange={(_, checked) => handleSelectAll(checked)}
                    />
                  }
                  label=""
                />
              </TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Filename</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Amount</TableCell>
              <TableCell>Description</TableCell>
              <TableCell>Date</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Confirmed</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {flatRows.map((row, idx) => {
              const { job, record, isFirstRecord, recordCount } = row;
              const selectable = selectableJobIds.includes(job.id);
              return (
                <TableRow key={`${job.id}-${record?.id ?? 'none'}-${idx}`} hover>
                  <TableCell padding="checkbox">
                    {isFirstRecord ? (
                      <Checkbox
                        checked={selected.includes(job.id)}
                        onChange={() => toggleSelect(job.id)}
                        disabled={!selectable}
                      />
                    ) : null}
                  </TableCell>
                  <TableCell>
                    {isFirstRecord ? (
                      <Chip label={job.status} color={statusColor(job.status)} size="small" />
                    ) : null}
                  </TableCell>
                  <TableCell>
                    {isFirstRecord ? (
                      <Box>
                        {job.originalFilename}
                        {recordCount > 1 && (
                          <Chip label={`${recordCount} items`} size="small" sx={{ ml: 1 }} variant="outlined" />
                        )}
                      </Box>
                    ) : null}
                  </TableCell>
                  <TableCell>
                    {isFirstRecord ? new Date(job.createdAt).toLocaleString() : null}
                  </TableCell>
                  <TableCell>
                    {record ? (
                      <Typography variant="body2" fontWeight={600}>
                        {formatExpenseAmount(record.amount)}
                      </Typography>
                    ) : (
                      isFirstRecord ? <Typography variant="body2" color="text.secondary">—</Typography> : null
                    )}
                  </TableCell>
                  <TableCell>
                    {record ? (
                      <Typography variant="caption" color="text.secondary">
                        {record.description}
                      </Typography>
                    ) : (
                      isFirstRecord && job.errorMessage ? (
                        <Typography variant="caption" color="error">{job.errorMessage}</Typography>
                      ) : null
                    )}
                  </TableCell>
                  <TableCell>{record ? record.expenseDate : null}</TableCell>
                  <TableCell>
                    {record?.categoryName ? (
                      <Chip
                        size="small"
                        label={record.categoryName}
                        icon={record.categoryIcon ? <span>{record.categoryIcon}</span> : undefined}
                        variant="outlined"
                      />
                    ) : (
                      record ? <Typography variant="body2" color="text.secondary">—</Typography> : null
                    )}
                  </TableCell>
                  <TableCell>
                    {record?.confirmed ? (
                      <Chip label="Confirmed" color="success" size="small" />
                    ) : record ? (
                      <Typography variant="body2" color="text.secondary">No</Typography>
                    ) : null}
                  </TableCell>
                  <TableCell align="right">
                    {record && !record.confirmed ? (
                      <Tooltip title="Edit">
                        <IconButton
                          size="small"
                          onClick={() => openEditDialog(record, job)}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    ) : null}
                  </TableCell>
                </TableRow>
              );
            })}
            {jobs.length === 0 && (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    No jobs yet. Upload files to get started.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={!!editingRecord} onClose={() => setEditingRecord(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Edit Temporary Record</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <TextField
            label="Amount"
            type="number"
            value={editForm.amount}
            onChange={(e) => setEditForm((prev) => ({ ...prev, amount: e.target.value }))}
            fullWidth
            sx={{ mb: 2 }}
          />
          <TextField
            label="Description"
            value={editForm.description}
            onChange={(e) => setEditForm((prev) => ({ ...prev, description: e.target.value }))}
            fullWidth
            sx={{ mb: 2 }}
          />
          <TextField
            label="Date"
            type="date"
            value={editForm.expenseDate}
            onChange={(e) => setEditForm((prev) => ({ ...prev, expenseDate: e.target.value }))}
            InputLabelProps={{ shrink: true }}
            fullWidth
            sx={{ mb: 2 }}
          />
          <CategorySelect
            value={editForm.categoryId}
            onChange={(id) => setEditForm((prev) => ({ ...prev, categoryId: id }))}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditingRecord(null)}>Cancel</Button>
          <Button variant="contained" onClick={handleEditSave} disabled={loading}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
