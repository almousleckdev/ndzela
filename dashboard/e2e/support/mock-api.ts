import type { Page, Route } from "@playwright/test";

type Body = unknown;
export interface MockRequest {
  method: string;
  path: string;
  query: URLSearchParams;
  // The JSON body; for a multipart upload, the JSON of its "data" part.
  body: Body;
  // The names of the files of a multipart upload.
  files: string[];
}
type Reply = Body | { status: number; body: Body };
type Handler = (request: MockRequest) => Reply;

const ORIGIN = "http://localhost:3100";
const CORS = { "access-control-allow-origin": ORIGIN, "access-control-allow-credentials": "true", "access-control-allow-headers": "*", "access-control-allow-methods": "*" };

const isReply = (value: unknown): value is { status: number; body: Body } =>
  typeof value === "object" && value !== null && "status" in value && "body" in value;

// Answers the dashboard's API calls from a table of "METHOD /path" handlers; anything else is a 404 and is listed in `unhandled`.
export class MockApi {
  readonly requests: MockRequest[] = [];
  readonly unhandled: string[] = [];
  private readonly handlers = new Map<string, Handler>();

  on(route: string, handler: Handler | Body): this {
    this.handlers.set(route, typeof handler === "function" ? (handler as Handler) : () => handler);
    return this;
  }

  async attach(page: Page): Promise<void> {
    await page.route("**/api/v1/**", (route) => this.answer(route));
  }

  requestsTo(route: string): MockRequest[] {
    return this.requests.filter((request) => `${request.method} ${request.path}` === route);
  }

  private async answer(route: Route): Promise<void> {
    const request = route.request();
    if (request.method() === "OPTIONS") return route.fulfill({ status: 204, headers: CORS });
    const url = new URL(request.url());
    const path = url.pathname.replace(/^\/api\/v1/, "");
    const call: MockRequest = { method: request.method(), path, query: url.searchParams, ...parseBody(request.headers()["content-type"] ?? "", request.postData()) };
    this.requests.push(call);
    const handler = this.handlers.get(`${call.method} ${path}`);
    if (!handler) {
      this.unhandled.push(`${call.method} ${path}`);
      return route.fulfill({ status: 404, headers: CORS, json: { status: 404, code: "RESOURCE_NOT_FOUND", message: "no mock", path } });
    }
    const reply = handler(call);
    const { status, body } = isReply(reply) ? reply : { status: 200, body: reply };
    return route.fulfill({ status, headers: CORS, json: body ?? {} });
  }
}

export const failure = (status: number, code: string, message = code) => ({ status, body: { status, code, message, path: "", timestamp: "" } });

function parseBody(contentType: string, raw: string | null): { body: Body; files: string[] } {
  if (!raw) return { body: null, files: [] };
  if (!contentType.startsWith("multipart/form-data")) {
    try {
      return { body: JSON.parse(raw), files: [] };
    } catch {
      return { body: null, files: [] };
    }
  }
  const files: string[] = [];
  let body: Body = null;
  for (const part of raw.split(/\r\n--/)) {
    const name = /name="([^"]+)"/.exec(part)?.[1];
    if (!name) continue;
    if (name === "data") body = JSON.parse(part.slice(part.indexOf("\r\n\r\n") + 4));
    else if (/filename="/.test(part)) files.push(name);
  }
  return { body, files };
}
