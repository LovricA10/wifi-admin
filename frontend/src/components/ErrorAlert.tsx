interface Props {
  message: string;
  onDismiss?: () => void;
}

export function ErrorAlert({ message, onDismiss }: Props) {
  return (
    <div className="error-alert" role="alert">
      <span>{message}</span>
      {onDismiss && (
        <button className="dismiss-btn" type="button" onClick={onDismiss} aria-label="Dismiss error">
          &times;
        </button>
      )}
    </div>
  );
}
