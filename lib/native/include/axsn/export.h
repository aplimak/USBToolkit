#pragma once

/* Visibility annotations for AXSN's compiled native features. */
#if defined(__clang__) || defined(__GNUC__)
#  define AXSN_EXPORT __attribute__((visibility("default")))
#  define AXSN_LOCAL  __attribute__((visibility("hidden")))
#else
#  define AXSN_EXPORT
#  define AXSN_LOCAL
#endif
