# Stage 1: install and build. NEXT_PUBLIC_* values are written into the bundle at build time, so the API address is a build argument.
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
ARG NEXT_PUBLIC_API_URL
ENV NEXT_PUBLIC_API_URL=$NEXT_PUBLIC_API_URL
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# Stage 2: run. Dependencies stay (next.config.ts needs TypeScript to load).
FROM node:22-alpine
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
COPY --from=build --chown=node:node /app/package.json ./
COPY --from=build --chown=node:node /app/node_modules ./node_modules
COPY --from=build --chown=node:node /app/.next ./.next
COPY --from=build --chown=node:node /app/public ./public
COPY --from=build --chown=node:node /app/next.config.ts ./
COPY --from=build --chown=node:node /app/tsconfig.json ./
USER node
EXPOSE 3000
# The host gives the port in PORT
CMD ["sh", "-c", "node_modules/.bin/next start -p ${PORT:-3000}"]
