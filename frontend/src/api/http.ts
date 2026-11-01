/** The RFC 9457 problem detail every service returns for an error. */
export interface ProblemDetail {
  status: number;
  detail?: string;
  fieldErrors?: Record<string, string>;
}

export class ApiError extends Error {
  readonly problem: ProblemDetail;

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? `The request failed with status ${problem.status}.`);
    this.name = 'ApiError';
    this.problem = problem;
  }
}

export async function getJson<T>(url: string): Promise<T> {
  return readJson<T>(await fetch(url));
}

export async function postJson<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
  return readJson<T>(response);
}

async function readJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new ApiError(await readProblem(response));
  }
  return (await response.json()) as T;
}

async function readProblem(response: Response): Promise<ProblemDetail> {
  const body = await response.json().catch(() => ({}));
  return { ...body, status: response.status };
}
