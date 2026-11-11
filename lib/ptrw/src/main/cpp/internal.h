//
// Created by aeliux on 11/11/26.
//

#ifndef PTRW_INTERNAL_H
#define PTRW_INTERNAL_H

/**
 * Resolves and validates a pointer not null and called from th owning thread.
 * @param env JNI env, used to queue a exception in case of any error.
 * @param ptr Pointer to the ptrw_t.
 * @return The resolved ptrw_t if it is valid and called from the owner thread, otherwise NULL.
 */
ptrw_t *ptrw_resolve(JNIEnv *env, long ptr);

#endif //PTRW_INTERNAL_H
