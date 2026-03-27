import { Notify, Confirm, Report } from 'notiflix'

/**
 * グローバル設定（可選）
 */
Notify.init({
  width: '320px',
  position: 'center-top',
  fontSize: '16px',
  timeout: 2500,
})

/**
 * Confirm（初期化）
 */
Confirm.init({
  titleColor: '#3b82f6',
  okButtonBackground: '#3b82f6',
  cancelButtonBackground: '#6b7280',
  borderRadius: '10px',
})
/**
 * Alert（エラー）
 */
export const alert = (message: string, title = 'WARNING') => {
  Report.warning(title, message, '閉じる')
}
/**
 * Confirm（確認）
 */
export const confirm = (
  message: string,
  title = 'CONFIRM'
): Promise<boolean> => {
  return new Promise((resolve) => {
    Confirm.show(
      title,
      message,
      'はい',
      'いいえ',
      () => resolve(true), 
      () => resolve(false)
    )
  })
}