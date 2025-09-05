package com.dotcms.ai.vision;

import com.dotcms.ai.translation.workflow.OpenAITranslationActionlet;
import com.dotcms.ai.vision.listener.OpenAIImageTaggingContentListener;
import com.dotcms.ai.vision.workflow.OpenAIVisionAutoTagActionlet;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.osgi.GenericBundleActivator;
import com.dotmarketing.portlets.workflows.actionlet.WorkFlowActionlet;
import com.dotmarketing.portlets.workflows.business.WorkflowAPI;
import com.dotmarketing.portlets.workflows.business.WorkflowAPIOsgiService;
import com.dotmarketing.util.Logger;
import io.vavr.Lazy;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator extends GenericBundleActivator {

    private static final OpenAIImageTaggingContentListener LISTENER = new OpenAIImageTaggingContentListener();


    public static Lazy<ThreadPoolExecutor> AIThreadPool = Lazy.of(() ->
            new ThreadPoolExecutor(10, 10, 60L, java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<>(10000),
                    new ThreadPoolExecutor.CallerRunsPolicy()));

    // Register the actionlets




    private final List<WorkFlowActionlet> actionlets = List.of(
            new OpenAIVisionAutoTagActionlet(),
            new OpenAITranslationActionlet()
    );


    public void start(BundleContext context) throws Exception {

       this.forceWorkflowServiceLoading(context);

       // Add the Image Tagging listener
       subscribeEmbeddingsListener();

        // Register Embedding Actionlet
       for( WorkFlowActionlet a : actionlets){
            Logger.info(this.getClass().getName(), " In activator: Registering Actionlet: " + a.getName());
          this.registerActionlet(context, a);
       }


        System.out.println("REALLY Starting OpenAI Vision Auto Tagging Actionlet");


    }

    public void stop(BundleContext context) throws Exception {

        unsubscribeEmbeddingsListener();

        // unregistering the actionlets actually removes them and their config from the system
        //this.unregisterActionlets();
        AIThreadPool.get().shutdown();
    }

    private void unsubscribeEmbeddingsListener() {
        APILocator.getLocalSystemEventsAPI().unsubscribe(LISTENER);
    }


    private void subscribeEmbeddingsListener() {

        APILocator.getLocalSystemEventsAPI().subscribe(LISTENER);

    }



   private void forceWorkflowServiceLoading ( BundleContext context ) {

      //Getting the service to register our Actionlet
      ServiceReference<?> serviceRefSelected = context.getServiceReference( WorkflowAPIOsgiService.class.getName() );
      if ( serviceRefSelected == null ) {

         //Forcing the loading of the WorkflowService
         WorkflowAPI workflowAPI = APILocator.getWorkflowAPI();
         if ( workflowAPI != null ) {

            serviceRefSelected = context.getServiceReference( WorkflowAPIOsgiService.class.getName() );
            if ( serviceRefSelected == null ) {
               //Forcing the registration of our required services
               workflowAPI.registerBundleService();
            }
         }
      }
   }



}
