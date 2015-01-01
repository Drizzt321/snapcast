#ifndef CLIENT_CONNECTION_H
#define CLIENT_CONNECTION_H

#include <string>
#include <thread>
#include <atomic>
#include <mutex>
#include <memory>
#include <boost/asio.hpp>
#include <condition_variable>
#include <set>
#include "message/message.h"
#include "common/timeDefs.h"


using boost::asio::ip::tcp;


class ClientConnection;


struct PendingRequest
{
	PendingRequest(uint16_t reqId) : id(reqId), response(NULL) {};

	uint16_t id;
	std::shared_ptr<msg::SerializedMessage> response;
	std::condition_variable cv;
};


class MessageReceiver
{
public:
	virtual void onMessageReceived(ClientConnection* connection, const msg::BaseMessage& baseMessage, char* buffer) = 0;
	virtual void onException(ClientConnection* connection, const std::exception& exception) = 0;
};


class ClientConnection
{
public:
	ClientConnection(MessageReceiver* receiver, const std::string& ip, size_t port);
	virtual ~ClientConnection();
	virtual void start();
	virtual void stop();
	virtual bool send(msg::BaseMessage* message);
	virtual std::shared_ptr<msg::SerializedMessage> sendRequest(msg::BaseMessage* message, const chronos::msec& timeout = chronos::msec(1000));

	template <typename T>
	std::shared_ptr<T> sendReq(msg::BaseMessage* message, const chronos::msec& timeout = chronos::msec(1000))
	{
		std::shared_ptr<msg::SerializedMessage> reply = sendRequest(message, timeout);
		if (!reply)
			return NULL;
		std::shared_ptr<T> msg(new T);
		msg->deserialize(reply->message, reply->buffer);
		return msg;
	}

	virtual bool active()
	{
		return active_;
	}

	virtual bool connected()
	{
		return (socket_ != 0);
//		return (connected_ && socket);
	}

protected:
	virtual void reader();

	void socketRead(void* to, size_t bytes);
	void getNextMessage();

	std::shared_ptr<tcp::socket> socket_;
	std::atomic<bool> active_;
	std::atomic<bool> connected_;
	MessageReceiver* messageReceiver_;
	mutable std::mutex mutex_;
	std::set<std::shared_ptr<PendingRequest>> pendingRequests_;
	uint16_t reqId_;
	std::string ip_;
	size_t port_;
	std::thread* readerThread_;
	chronos::msec sumTimeout_;
};



#endif




