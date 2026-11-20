//
// Created by aeliux on 11/19/26.
//

#define LOG_TAG "libptrw:PointerWrapper.cpp"

#include "PointerWrapper.hpp"
#include <axsn/log.h>
#include <stdexcept>
#include <utility>

namespace ptrw {

    struct PointerWrapper::Impl {
        void* pointer;
        Deleter native_deleter;
        std::shared_ptr<JniCallable> jni_deleter;
        const PointerWrapperFlags flags;
        const std::thread::id owner;
    };

    PointerWrapper::PointerWrapper(
            void* pointer,
            Deleter native_deleter,
            std::shared_ptr<JniCallable> jni_deleter,
            PointerWrapperFlags flags
    ): m_impl(nullptr) {
        if (pointer == nullptr) {
            throw std::invalid_argument(
                    "PointerWrapper cannot wrap a null pointer"
            );
        }

        LOGV("ctor(%p) -> %p", pointer, static_cast<void *>(this));

        m_impl = std::make_unique<Impl>(Impl{
                pointer,
                native_deleter,
                std::move(jni_deleter),
                flags,
                std::this_thread::get_id()
        });
    }

    PointerWrapper::~PointerWrapper() noexcept {
        if (isClosed()) return;
        LOGV("dtor(%p)", static_cast<void *>(this));
        close();
    }

    void PointerWrapper::close() {
        if (isClosed()) return;
        assertAllowedThread();

        LOGV("close(%p)", static_cast<void *>(this));
        void* pointer = m_impl->pointer;
        m_impl->pointer = nullptr;

        if (m_impl->native_deleter) m_impl->native_deleter(pointer);
        if (m_impl->jni_deleter) m_impl->jni_deleter->call();
    }

    void PointerWrapper::assertAllowedThread() const
    {
        if (isThreadConfined() && !isOwnerThread()) {
            throw std::runtime_error(
                    "PointerWrapper accessed from non-owner thread"
            );
        }
    }

    void PointerWrapper::assertOpen() const
    {
        if (isClosed()) {
            throw std::runtime_error(
                    "PointerWrapper is closed"
            );
        }
    }

    void *PointerWrapper::getRawPointer() const
    {
        assertOpen();
        assertAllowedThread();

        LOGD("getRawPointer %p from %p", m_impl->pointer, static_cast<const void *>(this));
        return m_impl->pointer;
    }

    bool PointerWrapper::isOwnerThread() const noexcept
    {
        return m_impl->owner == std::this_thread::get_id();
    }

    bool PointerWrapper::isClosed() const noexcept
    {
        return m_impl->pointer == nullptr;
    }

    bool PointerWrapper::isThreadConfined() const noexcept {
        return hasFlag(m_impl->flags, PointerWrapperFlags::ThreadConfined);
    }

    PointerWrapperFlags PointerWrapper::getFlags() const noexcept
    {
        return m_impl->flags;
    }
} // ptrw
