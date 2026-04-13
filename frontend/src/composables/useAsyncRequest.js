import { ref, shallowRef } from 'vue'

/**
 * 비동기 작업(API 호출 등)의 로딩·에러·결과를 한곳에서 다룬다.
 *
 * @template T
 * @param {(...args: unknown[]) => Promise<T>} asyncFn
 * @param {{ immediate?: boolean }} [options] immediate가 true이면 composable 생성 직후 run()을 한 번 호출한다.
 * @returns {{
 *   data: import('vue').ShallowRef<T | null>,
 *   loading: import('vue').Ref<boolean>,
 *   error: import('vue').Ref<Error | null>,
 *   run: (...args: unknown[]) => Promise<void>,
 *   reset: () => void
 * }}
 */
export function useAsyncRequest(asyncFn, options = {}) {
  /** @type {import('vue').ShallowRef<T | null>} */
  const data = shallowRef(null)
  const loading = ref(false)
  /** @type {import('vue').Ref<Error | null>} */
  const error = ref(null)

  /**
   * @param {...unknown} args asyncFn에 그대로 전달한다.
   */
  async function run(...args) {
    loading.value = true
    error.value = null
    try {
      const result = await asyncFn(...args)
      data.value = result
    } catch (cause) {
      error.value = cause instanceof Error ? cause : new Error(String(cause))
      data.value = null
    } finally {
      loading.value = false
    }
  }

  function reset() {
    data.value = null
    error.value = null
  }

  if (options.immediate) {
    void run()
  }

  return { data, loading, error, run, reset }
}
