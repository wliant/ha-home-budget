'use client';

import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  IconButton,
  Box,
  CircularProgress,
  Typography,
} from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import api from '@/services/api';

interface FilePreviewDialogProps {
  open: boolean;
  onClose: () => void;
  fileUrl: string | null;
  filename: string;
}

function isPdf(filename: string): boolean {
  return filename.toLowerCase().endsWith('.pdf');
}

export default function FilePreviewDialog({ open, onClose, fileUrl, filename }: FilePreviewDialogProps) {
  const [blobUrl, setBlobUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!open || !fileUrl) {
      setBlobUrl(null);
      setError('');
      return;
    }

    let objectUrl: string | null = null;
    setLoading(true);
    setError('');

    api.get(fileUrl, { responseType: 'blob' })
      .then((response) => {
        objectUrl = URL.createObjectURL(response.data);
        setBlobUrl(objectUrl);
      })
      .catch(() => {
        setError('Failed to load file preview.');
      })
      .finally(() => {
        setLoading(false);
      });

    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
      setBlobUrl(null);
    };
  }, [open, fileUrl]);

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg" fullWidth>
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', pr: 1 }}>
        <Typography variant="subtitle1" noWrap sx={{ maxWidth: '85%' }}>
          {filename}
        </Typography>
        <IconButton onClick={onClose} size="small">
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent sx={{ p: 0, minHeight: 200, position: 'relative' }}>
        {loading && (
          <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: 300 }}>
            <CircularProgress />
          </Box>
        )}
        {error && (
          <Box sx={{ p: 3, textAlign: 'center' }}>
            <Typography color="error">{error}</Typography>
          </Box>
        )}
        {blobUrl && isPdf(filename) && (
          <iframe
            src={blobUrl}
            style={{ width: '100%', height: '75vh', border: 'none' }}
            title={filename}
          />
        )}
        {blobUrl && !isPdf(filename) && (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              bgcolor: 'grey.100',
              p: 2,
              minHeight: 300,
            }}
          >
            <img
              src={blobUrl}
              alt={filename}
              style={{ maxWidth: '100%', maxHeight: '75vh', objectFit: 'contain' }}
            />
          </Box>
        )}
      </DialogContent>
    </Dialog>
  );
}
