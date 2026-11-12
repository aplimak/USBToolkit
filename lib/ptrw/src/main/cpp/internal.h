//
// Created by aeliux on 11/11/26.
//

#ifndef PTRW_INTERNAL_H
#define PTRW_INTERNAL_H

/**
 * Resolves ptrw and validates called from the owning thread.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param wptr Pointer to the ptrw_t.
 * @return The resolved ptrw_t if it is valid and called from the owner thread, otherwise NULL.
 * @note This function does not check closed pointer wrapper.
 */
ptrw_t *ptrw_resolve(JNIEnv *env, long wptr);

#endif //PTRW_INTERNAL_H
