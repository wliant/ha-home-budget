# Dockerfile for Next.js development

FROM node:18-alpine

WORKDIR /app

# Install dependencies first (for caching)
COPY package*.json ./
RUN npm ci

# Copy application files
COPY . .

# Expose Next.js default port
EXPOSE 3000

# Development mode with hot reload
CMD ["npm", "run", "dev"]

# For production build (not used in dev):
# RUN npm run build
# CMD ["npm", "start"]
