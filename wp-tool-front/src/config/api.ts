export const API_CONFIG = {
  BASE_URL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
  ENDPOINTS: {
    GENERATE_IO_CODE: import.meta.env.VITE_GENERATE_IO_CODE || '/api/v1/excel/generate-io-code',
    GENERATE_DB_QUERY_CODE: import.meta.env.VITE_GENERATE_DB_QUERY_CODE || '/api/v1/excel/generate-db-query-code',
  }
}

export const getApiUrl = (endpoint: string): string => {
  return `${API_CONFIG.BASE_URL}${endpoint}`
}