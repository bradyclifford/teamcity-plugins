package com.goldin.plugins.teamcity.messenger.impl

import com.goldin.plugins.teamcity.messenger.api.MessagesContext
import com.goldin.plugins.teamcity.messenger.api.MessagesPersistency
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import jetbrains.buildServer.serverSide.ServerPaths
import org.gcontracts.annotations.Requires
import org.gcontracts.annotations.Invariant


/**
 * {@link MessagesPersistency} implementation
 */
@Invariant({
    this.dataDirectory.directory &&
    this.context                 &&
    this.jsonFile.file           &&
    this.jsonFile.canWrite()
})
class MessagesPersistencyImpl implements MessagesPersistency
{
    private final File            dataDirectory
    private final MessagesContext context
    private final File            jsonFile


    @Requires({ context && paths })
    MessagesPersistencyImpl ( MessagesContext context, ServerPaths paths )
    {
        this.dataDirectory = new File( paths.pluginDataDirectory, context.pluginName )
        this.context       = context
        this.jsonFile      = new File( dataDirectory, 'messages.json' )

        if ( ! dataDirectory.isDirectory()) { assert dataDirectory.mkdirs()  }
        if ( ! jsonFile.isFile())           { assert jsonFile.createNewFile()}
    }


    @Override
    void save ( Map data )
    {
        try
        {
            jsonFile.write( new JsonBuilder( data ).toString())
        }
        catch ( e )
        {
            context.log.error( "Failed to persist [$data] to [$jsonFile.canonicalPath]: $e", e )
        }
    }


    @Override
    Map restore ()
    {
        def jsonData = jsonFile.text

        try
        {
            return ( jsonData ? ( Map ) new JsonSlurper().parseText( jsonData ) : [:] )
        }
        catch ( e )
        {
            def copyFile = new File( dataDirectory, "messages-failed-to-load-${ System.currentTimeMillis() }.json" )
            assert jsonFile.renameTo( copyFile )

            context.log.error( "Failed to restore JSON data from [$jsonFile.canonicalPath], copied to [$copyFile.canonicalPath]: $e", e )
            return [:]
        }
    }
}
