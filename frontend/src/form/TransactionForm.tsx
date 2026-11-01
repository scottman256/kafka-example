import type { ChangeEvent, ReactNode } from 'react';
import type { TransactionDraft } from './draft';
import type { ResultOutcome } from './submit';
import { useSourceBanks } from './useSourceBanks';
import { useTransactionSubmission } from './useTransactionSubmission';
import type { FieldErrors } from './validation';

interface TransactionFormProps {
  draft: TransactionDraft;
  onDraftChange: (draft: TransactionDraft) => void;
  onOutcome: (outcome: ResultOutcome) => void;
}

export function TransactionForm({ draft, onDraftChange, onOutcome }: TransactionFormProps) {
  const { errors, submitting, handleSubmit } = useTransactionSubmission(draft, onOutcome);
  return (
    <form className="card transaction-form" onSubmit={handleSubmit} noValidate>
      <h2>New transaction</h2>
      <FormFields draft={draft} errors={errors} onDraftChange={onDraftChange} />
      <button type="submit" disabled={submitting}>
        {submitting ? 'Sending…' : 'Send transaction'}
      </button>
    </form>
  );
}

interface FormFieldsProps {
  draft: TransactionDraft;
  errors: FieldErrors;
  onDraftChange: (draft: TransactionDraft) => void;
}

function FormFields({ draft, errors, onDraftChange }: FormFieldsProps) {
  const { banks, failed } = useSourceBanks();
  const bind = (name: keyof TransactionDraft) => ({
    id: name,
    name,
    value: draft[name],
    'aria-invalid': errors[name] !== undefined,
    'aria-describedby': errors[name] && `${name}-error`,
    onChange: (event: ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
      onDraftChange({ ...draft, [name]: event.target.value }),
  });
  const bankError = errors.sourceBank ?? (failed ? 'Could not load the banks from transaction-service.' : undefined);

  return (
    <div className="fields">
      <Field label="Date" name="transactionDate" error={errors.transactionDate}>
        <input type="date" {...bind('transactionDate')} />
      </Field>
      <Field label="Amount (USD)" name="amount" error={errors.amount}>
        <input type="text" inputMode="decimal" placeholder="125.50" {...bind('amount')} />
      </Field>
      <Field label="Payer" name="payer" error={errors.payer}>
        <input type="text" placeholder="Who is paying" {...bind('payer')} />
      </Field>
      <Field label="Payee" name="payee" error={errors.payee}>
        <input type="text" placeholder="Who is being paid" {...bind('payee')} />
      </Field>
      <Field label="Memo" name="memo" error={errors.memo}>
        <input type="text" placeholder="What it's for (optional)" {...bind('memo')} />
      </Field>
      <Field label="Source bank" name="sourceBank" error={bankError}>
        <select {...bind('sourceBank')}>
          <option value="">Choose a bank</option>
          {banks.map((bank) => (
            <option key={bank}>{bank}</option>
          ))}
        </select>
      </Field>
    </div>
  );
}

interface FieldProps {
  label: string;
  name: keyof TransactionDraft;
  error?: string;
  children: ReactNode;
}

function Field({ label, name, error, children }: FieldProps) {
  return (
    <div className="field">
      <label htmlFor={name}>{label}</label>
      {children}
      {error && (
        <p className="field__error" id={`${name}-error`}>
          {error}
        </p>
      )}
    </div>
  );
}
