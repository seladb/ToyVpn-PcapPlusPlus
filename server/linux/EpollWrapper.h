#pragma once

#include <functional>
#include <sys/epoll.h>
#include <unistd.h>
#include <unordered_map>

class EPollWrapper {
  public:
    using EPollCallback = std::function<void(int)>;

    virtual ~EPollWrapper() {
        if (m_EPollFd != -1) {
            close(m_EPollFd);
        }
    }

    void init(int maxEvents) {
        m_EPollFd = epoll_create1(0);
        if (m_EPollFd == -1) {
            throw std::runtime_error("Error creating epoll instance!");
        }

        m_MaxEvents = maxEvents;
    }

    void add(int fd, const EPollCallback &callback) {
        if (m_EPollFd == -1) {
            throw std::runtime_error(
                "Instance not initialized, please call init()!");
        }

        struct epoll_event event;
        event.events = EPOLLIN;
        event.data.fd = fd;
        if (epoll_ctl(m_EPollFd, EPOLL_CTL_ADD, fd, &event) == -1) {
            throw std::runtime_error("Error adding fd to epoll!");
        }

        m_FdToCallbackMap[event.data.fd] = callback;
    }

    void remove(int fd) { m_FdToCallbackMap.erase(fd); }

    void startPolling() {
        if (m_IsPolling) {
            throw std::runtime_error("Already polling!");
        }

        m_IsPolling = true;

        epoll_event events[m_MaxEvents];

        while (m_IsPolling) {
            int numEvents = epoll_wait(m_EPollFd, events, m_MaxEvents, -1);
            if (numEvents < 0) {
                if (!m_IsPolling) {
                    return;
                }
                m_IsPolling = false;
                throw std::runtime_error("Error with epoll_wait!");
            }

            for (int i = 0; i < numEvents; ++i) {
                m_FdToCallbackMap[events[i].data.fd](events[i].data.fd);
            }
        }
    }

    void stopPolling() {
        m_IsPolling = false;
        close(m_EPollFd);
    }

  private:
    int m_EPollFd = -1;
    int m_MaxEvents = -1;
    std::unordered_map<int, EPollCallback> m_FdToCallbackMap;
    bool m_IsPolling = false;
};
