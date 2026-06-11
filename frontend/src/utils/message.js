import { ElMessage } from 'element-plus'

const MESSAGE_OFFSET = 84

export function showErrorMessage(message) {
  ElMessage({
    message,
    type: 'error',
    offset: MESSAGE_OFFSET
  })
}

export function showSuccessMessage(message) {
  ElMessage({
    message,
    type: 'success',
    offset: MESSAGE_OFFSET
  })
}
