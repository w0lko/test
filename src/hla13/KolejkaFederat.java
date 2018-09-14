package hla13;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import hla.rti.AttributeHandleSet;
import hla.rti.FederatesCurrentlyJoined;
import hla.rti.FederationExecutionAlreadyExists;
import hla.rti.FederationExecutionDoesNotExist;
import hla.rti.LogicalTime;
import hla.rti.LogicalTimeInterval;
import hla.rti.RTIambassador;
import hla.rti.RTIexception;
import hla.rti.ResignAction;
import hla.rti.SuppliedAttributes;
import hla.rti.SuppliedParameters;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

public class KolejkaFederat
{
    public static final int ITERATIONS = 20;

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private KolejkaAmbasador fedamb;

    private void log(String message)
    {
        System.out.println( "LOG KolejkaFederat: " + message );
    }

    private void waitForUser(String komunikat)
    {
        log(komunikat);
        try
        {
            new BufferedReader( new InputStreamReader(System.in) ).readLine();
        }
        catch( Exception e )
        {
            log( "Błąd przy interakcji użytkownika " + e.getMessage() );
            //e.printStackTrace();
        }
    }

    private LogicalTime convertTime( double time )
    {
        return new DoubleTime( time ); // PORTICO SPECIFIC!!
    }

    private LogicalTimeInterval convertInterval( double time )
    {
        return new DoubleTimeInterval( time ); // PORTICO SPECIFIC!!
    }

    public void runFederate( String federateName ) throws RTIexception
    {
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

        try
        {
            File fom = new File( "testfom.fed" );
            rtiamb.createFederationExecution( "FEDERACJA", fom.toURI().toURL() );
            log( "Utworzono federację" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Nie utworzono federacji, ponieważ istnieje" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception processing fom: " + urle.getMessage() ); // urle.printStackTrace();
            return;
        }

        fedamb = new KolejkaAmbasador();
        rtiamb.joinFederationExecution( federateName, "FEDERACJA", fedamb );
        log( "Dołączono federata do federacji: " + federateName );

        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        // wait until the point is announced
        while( fedamb.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser("########## ROZPOCZNIJ SYMULACJĘ ##########");

        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Osiągnięto punkt synchronizacji " +READY_TO_RUN+ ", oczekiwanie na federację..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.tick();
        }

        enableTimePolicy();
        log( "Time Policy Enabled" );

        publishAndSubscribe();
        log( "RTI wie, że federat jest gotowy na subskrybcję i federację" );

        int objectHandle = registerObject();
        log( "Zarejestrowano obiekt, handle=" + objectHandle );

        for( int i = 0; i < ITERATIONS; i++ )
        {
            // 9.1 update the attribute values of the instance //
            updateAttributeValues( objectHandle );
            log( "Zaktualizowano adrybuty w iteracji "  + i+"/"+ ITERATIONS);
            // 9.2 send an interaction
            sendInteraction();
            log( "Wysłano interakcę " + i+"/"+ ITERATIONS);
            // 9.3 request a time advance and wait until we get it
            advanceTime( 1.0 );
            log( "Czas " + fedamb.federateTime );
        }

        deleteObject( objectHandle );
        log( "Usunięcie obiektu, handle=" + objectHandle );

        rtiamb.resignFederationExecution( ResignAction.NO_ACTION );
        log( "Wyrejestrowano z federacji" );

        try
        {
            rtiamb.destroyFederationExecution( "FEDERACJA" );
            log( "Usunięto federację" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            log( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            log( "Didn't destroy federation, federates still joined" );
        }
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = convertTime( fedamb.federateTime );
        LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( fedamb.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( fedamb.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception
    {
        int classHandle = rtiamb.getObjectClassHandle( "ObjectRoot.Kolejka" );
        int aaHandle    = rtiamb.getAttributeHandle( "rozmiar", classHandle );
        AttributeHandleSet attributes = RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( aaHandle );
        rtiamb.publishObjectClass( classHandle, attributes );

        rtiamb.subscribeObjectClassAttributes( classHandle, attributes );

        int interactionHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.X" );

        rtiamb.publishInteractionClass( interactionHandle );

        rtiamb.subscribeInteractionClass( interactionHandle );
    }

    private int registerObject() throws RTIexception
    {
        int classHandle = rtiamb.getObjectClassHandle( "ObjectRoot.Kolejka" );
        return rtiamb.registerObjectInstance( classHandle );
    }

    private void updateAttributeValues( int objectHandle ) throws RTIexception
    {
        SuppliedAttributes attributes = RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

        byte[] aaValue = EncodingHelpers.encodeString( "rozmiar" + getLbts() );

        int classHandle = rtiamb.getObjectClass( objectHandle );
        int aaHandle = rtiamb.getAttributeHandle( "rozmiar", classHandle );

        attributes.add( aaHandle, aaValue );

        rtiamb.updateAttributeValues( objectHandle,attributes, generateTag() );

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        rtiamb.updateAttributeValues( objectHandle, attributes, generateTag(), time );
    }

    private void sendInteraction() throws RTIexception
    {
        SuppliedParameters parameters = RtiFactoryFactory.getRtiFactory().createSuppliedParameters();

        byte[] xaValue = EncodingHelpers.encodeDouble(getLbts());

        int classHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.X" );
        int xaHandle = rtiamb.getParameterHandle( "xa", classHandle );

        parameters.add( xaHandle, xaValue );

        rtiamb.sendInteraction( classHandle, parameters, generateTag() );

        LogicalTime time = convertTime( fedamb.federateTime + fedamb.federateLookahead );
        rtiamb.sendInteraction( classHandle, parameters, generateTag(), time );
    }

    private void advanceTime( double timestep ) throws RTIexception
    {
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );

        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }

    private void deleteObject( int handle ) throws RTIexception
    {
        rtiamb.deleteObjectInstance( handle, generateTag() );
    }

    private double getLbts()
    {
        return fedamb.federateTime + fedamb.federateLookahead;
    }

    private byte[] generateTag()
    {
        return (""+System.currentTimeMillis()).getBytes();
    }

    public static void main( String[] args )
    {
        String federateName = "KolejkaFederat";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            new KolejkaFederat().runFederate( federateName );
        }
        catch( RTIexception rtie )
        {
            rtie.printStackTrace();
        }
    }
}