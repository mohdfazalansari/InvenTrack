import React from 'react';

export default function Alert({ type = 'error', message, onClose }) {
  if (!message) return null;

  const bgColors = {
    error: 'bg-danger-light text-danger border-danger',
    success: 'bg-success-light text-success border-success',
    warning: 'bg-warning-light text-warning border-warning',
    info: 'bg-info-light text-info border-info',
  };

  const icons = {
    error: '⚠️',
    success: '✅',
    warning: '⚡',
    info: 'ℹ️',
  };

  return (
    <div className={`alert-box ${bgColors[type] || bgColors.error}`}>
      <span className="alert-icon">{icons[type] || 'ℹ️'}</span>
      <div className="alert-content">{message}</div>
      {onClose && (
        <button type="button" className="alert-close" onClick={onClose}>
          ✕
        </button>
      )}
    </div>
  );
}
