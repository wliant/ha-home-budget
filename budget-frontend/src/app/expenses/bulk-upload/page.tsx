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
  const [editingJob, setEditingJob] = useState<ExpenseInputJobDTO | null>(null);
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

  const selectableJobs = useMemo(
    () =>
      jobs.filter(
        (job) =>
          job.temporaryRecord &&
          !job.temporaryRecord.confirmed &&
          job.status === 'COMPLETED'
      ),
    [jobs]
  );

  const toggleSelect = (jobId: number) => {
    setSelected((prev) =>
      prev.includes(jobId) ? prev.filter((id) => id !== jobId) : [...prev, jobId]
    );
  };

  const handleSelectAll = (checked: boolean) => {
    if (checked) {
      setSelected(selectableJobs.map((job) => job.id));
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

  const openEditDialog = (job: ExpenseInputJobDTO) => {
    const record = job.temporaryRecord;
    if (!record || record.confirmed) return;
    setEditingJob(job);
    setEditForm({
      amount: record.amount.toString(),
      description: record.description,
      expenseDate: record.expenseDate,
      categoryId: record.categoryId ?? null,
    });
  };

  const handleEditSave = async () => {
    if (!editingJob) return;
    const record = editingJob.temporaryRecord;
    if (!record) return;
    setLoading(true);
    try {
      await expenseInputJobService.updateTemporaryRecord(editingJob.id, {
        amount: parseFloat(editForm.amount),
        description: editForm.description.trim(),
        expenseDate: editForm.expenseDate,
        categoryId: editForm.categoryId,
      });
      setEditingJob(null);
      await fetchJobs();
    } catch (err) {
      console.error('Failed to update temporary record', err);
      setError('Failed to update record.');
    } finally {
      setLoading(false);
    }
  };

  const renderTemp = (record?: TemporaryExpenseRecordDTO | null) => {
    if (!record) {
      return (
        <Typography variant="body2" color="text.secondary">
          —
        </Typography>
      );
    }
    return (
      <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        <Typography variant="body2" fontWeight={600}>
          {formatExpenseAmount(record.amount)}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          {record.description}
        </Typography>
      </Box>
    );
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
                      checked={selected.length > 0 && selected.length === selectableJobs.length}
                      indeterminate={selected.length > 0 && selected.length < selectableJobs.length}
                      onChange={(_, checked) => handleSelectAll(checked)}
                    />
                  }
                  label=""
                />
              </TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Filename</TableCell>
              <TableCell>Created</TableCell>
              <TableCell>Temp Record</TableCell>
              <TableCell>Date</TableCell>
              <TableCell>Category</TableCell>
              <TableCell>Confirmed</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {jobs.map((job) => {
              const record = job.temporaryRecord;
              const selectable = record && !record.confirmed && job.status === 'COMPLETED';
              return (
                <TableRow key={job.id} hover>
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={selected.includes(job.id)}
                      onChange={() => toggleSelect(job.id)}
                      disabled={!selectable}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip label={job.status} color={statusColor(job.status)} size="small" />
                  </TableCell>
                  <TableCell>{job.originalFilename}</TableCell>
                  <TableCell>{new Date(job.createdAt).toLocaleString()}</TableCell>
                  <TableCell>{renderTemp(record)}</TableCell>
                  <TableCell>{record ? record.expenseDate : '—'}</TableCell>
                  <TableCell>
                    {record?.categoryName ? (
                      <Chip
                        size="small"
                        label={record.categoryName}
                        icon={record.categoryIcon ? <span>{record.categoryIcon}</span> : undefined}
                        variant="outlined"
                      />
                    ) : (
                      <Typography variant="body2" color="text.secondary">—</Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    {record?.confirmed ? (
                      <Chip label="Confirmed" color="success" size="small" />
                    ) : (
                      <Typography variant="body2" color="text.secondary">No</Typography>
                    )}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title={record?.confirmed ? 'Already confirmed' : 'Edit'}>
                      <span>
                        <IconButton
                          size="small"
                          disabled={!record || record.confirmed}
                          onClick={() => openEditDialog(job)}
                        >
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              );
            })}
            {jobs.length === 0 && (
              <TableRow>
                <TableCell colSpan={9} align="center" sx={{ py: 4 }}>
                  <Typography variant="body2" color="text.secondary">
                    No jobs yet. Upload files to get started.
                  </Typography>
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <Dialog open={!!editingJob} onClose={() => setEditingJob(null)} maxWidth="sm" fullWidth>
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
          <Button onClick={() => setEditingJob(null)}>Cancel</Button>
          <Button variant="contained" onClick={handleEditSave} disabled={loading}>
            Save
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
}
