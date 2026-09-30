export interface ValidationError {
  field: string;
  message: string;
}

export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  requestId?: string;
  errors?: ValidationError[];
}

export class ApiException extends Error {
  public readonly response: ApiErrorResponse;
  public readonly status: number;

  constructor(response: ApiErrorResponse) {
    super(response.message);
    this.name = "ApiException";
    this.response = response;
    this.status = response.status;
  }
}
