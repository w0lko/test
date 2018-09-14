package hla13;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.ReflectedAttributes;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;

import org.portico.impl.hla13.types.DoubleTime;

public class KlientAmbasador extends NullFederateAmbassador
{
    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    public KlientAmbasador()
    {
    }

    private double convertTime( LogicalTime logicalTime )
    {
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println( "LOG KlientAmbasador: " + message );
    }

    public void synchronizationPointRegistrationFailed( String label )
    {
        log("Nie osiągnięto punktu synchronizacji: " + label );
    }

    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Udało się zarejestrować punkt synchronizacji: " + label );
    }

    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Zapowiadam punkt synchronizacji: " + label );
        if( label.equals(KlientFederat.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    public void federationSynchronized( String label )
    {
        log( "Federacja zsynchronizowana: " + label );
        if( label.equals(KlientFederat.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    public void timeRegulationEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isConstrained = true;
    }

    public void timeAdvanceGrant( LogicalTime theTime )
    {
        this.federateTime = convertTime( theTime );
        this.isAdvancing = false;
    }

    public void discoverObjectInstance( int theObject,
                                        int theObjectClass,
                                        String objectName )
    {
        log( "Odkryto obiekt: obiekt=" + theObject + ", klasa=" +
                theObjectClass + ", nazwa=" + objectName );
    }

    public void reflectAttributeValues( int theObject,
                                        ReflectedAttributes theAttributes,
                                        byte[] tag )
    {
        reflectAttributeValues( theObject, theAttributes, tag, null, null );
    }

    public void reflectAttributeValues( int theObject,
                                        ReflectedAttributes theAttributes,
                                        byte[] tag,
                                        LogicalTime theTime,
                                        EventRetractionHandle retractionHandle )
    {
        StringBuilder builder = new StringBuilder( "Reflection for object:" );

        builder.append( " obiekt=" + theObject );
        builder.append( ", tag=" + EncodingHelpers.decodeString(tag) );
        if( theTime != null )
        {
            builder.append( ", czas=" + convertTime(theTime) );
        }

        builder.append( ", liczba atrybutów=" + theAttributes.size() );
        builder.append( "\n" );
        for( int i = 0; i < theAttributes.size(); i++ )
        {
            try
            {
                builder.append( "\tattributeHandle=" );
                builder.append( theAttributes.getAttributeHandle(i) );
                builder.append( ", attributeValue=" );
                builder.append(
                        EncodingHelpers.decodeString(theAttributes.getValue(i)) );
                builder.append( "\n" );
            }
            catch( ArrayIndexOutOfBounds aioob )
            {

            }
        }

        log( builder.toString() );
    }

    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag )
    {
        receiveInteraction( interactionClass, theInteraction, tag, null, null );
    }

    public void receiveInteraction( int interactionClass, ReceivedInteraction theInteraction, byte[] tag, LogicalTime theTime, EventRetractionHandle eventRetractionHandle )
    {
        /*
        StringBuilder builder = new StringBuilder( "Interaction Received:" );
        builder.append( " handle=" + interactionClass );
        builder.append( ", tag=" + EncodingHelpers.decodeString(tag) );
        if( theTime != null )
        {
            builder.append( ", time=" + convertTime(theTime) );
        }

        builder.append( ", parameterCount=" + theInteraction.size() );
        for( int i = 0; i < theInteraction.size(); i++ )
        {
            try
            {
                builder.append( ", paramHandle=" );
                builder.append( theInteraction.getParameterHandle(i) );
                builder.append( ", paramValue=" );
                builder.append(EncodingHelpers.decodeDouble(theInteraction.getValue(i)));
                builder.append( "\n" );
            }
            catch( ArrayIndexOutOfBounds aioob )
            {
            }
        }
         log( builder.toString() )
        */
        for( int i = 0; i < theInteraction.size(); i++ ) {
            try
            {
                if (theTime != null) {
                    System.out.println("LOG KlientAmbasador: Odebranie interakcji. Wartość: " + EncodingHelpers.decodeDouble(theInteraction.getValue(i)));
                }
            }
            catch( ArrayIndexOutOfBounds aioob )
            {
            }
        }
    }

    public void removeObjectInstance( int theObject, byte[] userSuppliedTag )
    {
        log( "Obiekt usunięty: obiekt=" + theObject );
    }

    public void removeObjectInstance( int theObject,
                                      byte[] userSuppliedTag,
                                      LogicalTime theTime,
                                      EventRetractionHandle retractionHandle )
    {
        log( "Obiekt usunięty: obiekt=" + theObject );
    }
}
