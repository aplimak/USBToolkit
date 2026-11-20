//
// Created by aeliux on 11/19/26.
//

#include "PointerWrapper.hpp"
#include <stdexcept>
#include <utility>

namespace ptrw {

    struct PointerWrapper::Impl {
        void* pointer;
        Deleter native_deleter;
        std::shared_ptr<JniCallable> jni_deleter;
        const std::thread::id owner;
    };

    PointerWrapper::PointerWrapper(
            void* pointer,
            Deleter native_deleter,
            std::shared_ptr<JniCallable> jni_deleter
    ): m_impl(nullptr) {
        if (pointer == nullptr) {
            throw std::invalid_argument(
                    "PointerWrapper cannot wrap a null pointer"
            );
        }

        m_impl = new Impl{
                pointer,
                native_deleter,
                std::move(jni_deleter),
                std::this_thread::get_id()
        };
    }

    PointerWrapper::~PointerWrapper() noexcept {
        if (isClosed()) return;
        if (!isOwnerThread()) {
            std::terminate();
        }
        release();
        delete m_impl;
    }

    void PointerWrapper::assertOwnerThread() const
    {
        if (!isOwnerThread()) {
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

    void PointerWrapper::close() {
        assertOwnerThread();
        release();
    }

    void PointerWrapper::release() noexcept {
        if (isClosed())
            return;

        void* pointer = m_impl->pointer;
        m_impl->pointer = nullptr;

        if (m_impl->native_deleter) m_impl->native_deleter(pointer);
        if (m_impl->jni_deleter) m_impl->jni_deleter->call();
    }

    void *PointerWrapper::getRawPointer() const
    {
        assertOpen();
        assertOwnerThread();
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
} // ptrw
