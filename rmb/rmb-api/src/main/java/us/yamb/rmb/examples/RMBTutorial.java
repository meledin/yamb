/**
 * 
 */
/**
 * @author vladi
 */
package us.yamb.rmb.examples;

import us.yamb.mb.callbacks.AsyncResult;
import us.yamb.rmb.RMB;
import us.yamb.rmb.Request.Reply;
import us.yamb.rmb.builders.RMBBuilder;

/**
 * This example shows basic RMB functionality and how to use. See the source code.
 * <p>
 * The following classes are referenced:
 * <ul>
 *  <li> {@link us.yamb.rmb.examples}
 *  <li> {@link RMBBuilder}
 *  <li> {@link RMB}
 *  <li> {@link AsyncResult}
 * </ul>
 * @author Vladimir Katardjiev
 *
 */
public class RMBTutorial
{
    public static void main(String[] args) throws Exception
    {
        // RMB is instantiated through the use of a builder. The builder takes the parameters, and constructs a new instance.
        RMB rmb = RMBBuilder.builder().seed("http://messagebroker.rmb.example").id("myPreferredName").build();
        
        // RMB will not connect automatically, to give the application a chance to set callbacks. The callbacks can be found in the
        // us.yamb.rmb.callbacks package.
        
        // RMB.connect() is an asynchronous method, but it can be used in a synchronous manner.
        // See us.yamb.mb.callbacks.AsyncResult for more information.
        Exception err = rmb.connect().get();
        if (err != null)
            throw err;
        
        // Making a REST-style request is also done using a builder. The RMB.get() method here will create a new GET message builder for us.
        // The request is performed using the execute() command on the builder, which returns an AsyncResult. We once more
        // opt to make this a synchronous request.
        Reply reply = rmb.get("/server/news/list").execute().get();
        
        // This prints out the text body of the reply.
        System.out.println(reply.string());
        
        // We can also perform a fully asynchronous operation. This line will not block, but will call the lambda once a reply is received.
        rmb.get("/server/news/list").execute(asyncReply -> System.out.println(asyncReply.string()));
        
        // If we have no interest in the answer, we can use the message method to fire off a message with no reply.
        rmb.message().to("/server/comments/post").data("I like this").send();
        
        // We can directly request JSON objects. The objects will be deserialized using the fields as names.
        RMBTutorial jsonReply = rmb.get("/server/news/json").execute().get().object(RMBTutorial.class);
        
        // RMB can also listen for incoming messages. It will do so at the following path:
        String myPath = rmb.id();
        
        System.out.println(myPath); // Will normally print "/myPreferredName" unless it was taken/disallowed by the underlying implementation
        
        // This will listen for messages on /myPreferredName and print out the string data
        rmb.onpost(postRequest -> System.out.println(postRequest.string()));
        
        // This will send a message to the above listener, which will print out "Hello".
        // Here we use the generic builder.
        rmb.message().to(rmb.id()).data("Hello").method("POST").send();
        
        // RMB can also be used to create resource trees. For example, the following code places a listener at a given path
        // It also immediately assigns a listener to it.
        RMB res = rmb.create("/print").onpost(postRequest -> System.err.println(postRequest.string()));
        
        // This will send a message to the above listener, which will print out "Hello" in stderr.
        rmb.post(res.id()).data("Hi there").send();
        
        
    }
}