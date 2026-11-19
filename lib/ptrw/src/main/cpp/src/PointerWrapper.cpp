//
// Created by aeliux on 11/19/26.
//

#include "PointerWrapper.hpp"
#include <stdexcept>

namespace ptrw {

    struct PointerWrapper::Impl {
        void* pointer;
        Deleter deleter;
        const std::thread::id owner;
    };

    PointerWrapper::PointerWrapper(
            void* pointer,
            Deleter deleter
    ): m_impl(nullptr) {
        if (pointer == nullptr) {
            throw std::invalid_argument(
                    "PointerWrapper cannot wrap a null pointer"
            );
        }
        if (deleter == nullptr) {
            throw std::invalid_argument("Deleter cannot be null");
        }


        m_impl = new Impl{
                pointer,
                deleter,
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

        m_impl->deleter(pointer);
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
