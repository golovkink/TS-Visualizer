package app.streamAnalyzer;

import model.packet.Packet;
import model.pes.PES;

import java.math.BigInteger;

import static app.streamAnalyzer.TimestampParser.parsePTSdts;


class PESparser extends Parser {


    PESparser(){
    }


    PES analyzePES(Packet analyzedHeader, byte[] packet) {

        int position = calculatePosition(analyzedHeader);

        int PESlength = tsPacketSize - position;
        int[] PESFields = parseNfields(packet, position, PESlength);
        byte[] binaryPESFields = intToBinary(PESFields, PESlength );

        if (position < tsPacketSize - packetStartCodePrefixLength ) {
            int pscp = (int) binToInt(binaryPESFields, position=0, position += packetStartCodePrefixLength);

            if (pscp == packetStartCodePrefix) {

                int streamID = (int) binToInt(binaryPESFields, position, position += streamIDlength);
                int PESpacketLength = (int) binToInt(binaryPESFields, position, position += PESpacketLengthLength);
                byte PESscramblingControl = (byte) binToInt(binaryPESFields, position, position += PESscramblingControlLength);
                byte PESpriority = (byte) binToInt(binaryPESFields, position += 2, position += PESpriorityLength);
                byte DataAlignmentIndicator = (byte) binToInt(binaryPESFields, position, position += DataAlignmentIndicatorLength);
                byte copyright = (byte) binToInt(binaryPESFields, position, position += copyrightLength);
                byte OriginalOrCopy = (byte) binToInt(binaryPESFields, position, position += OriginalOrCopyLength);

                tables.updateStreamCodes(analyzedHeader.getPID(), Integer.valueOf(streamID));
                tables.updatePacketsSizeMap(analyzedHeader.getPID(), PESpacketLength);

                return analyzePESoptionalHeader(
                        new PES(
                                streamID,
                                PESpacketLength,
                                PESscramblingControl,
                                PESpriority,
                                DataAlignmentIndicator,
                                copyright,
                                OriginalOrCopy
                        ) , binaryPESFields, position, analyzedHeader.getPID(), analyzedHeader.getIndex()
                );


            }
        }
        return new PES();
    }


    private PES analyzePESoptionalHeader(PES header, byte[] binaryPESFields, int position, int pid, long packetIndex) {

        byte PTSdtsFlags = (byte) binToInt(binaryPESFields, position, position += PTSdtsFlagsLength);
        byte ESCRflag = (byte) binToInt(binaryPESFields, position, position += PESCRflagLength);
        byte ESrateFlag = (byte) binToInt(binaryPESFields, position, position += ESrateFlagLength);
        byte DSMtrickModeFlag = (byte) binToInt(binaryPESFields, position, position += DSMtrickModeFlagLength);
        byte AdditionalCopyInfoFlag = (byte) binToInt(binaryPESFields, position, position += AdditionalCopyInfoFlagLength);
        byte PEScrcFlag = (byte) binToInt(binaryPESFields, position, position += PEScrcFlagLength);
        byte PESextensionFlag = (byte) binToInt(binaryPESFields, position, position += PESextensionFlagLength);

        int PESheaderDataLength = (int) binToInt(binaryPESFields, position, position += PESheaderDataLengthLength);

        long PTS = nil;
        long DTS = nil;
        long ESCR = nil;
        long ESrate = nil;
        int DSMtrickMode = nil;
        int AdditionalCopyInfo = nil;
        long PEScrc = nil;

        if(PTSdtsFlags > 1) {
            if(PTSdtsFlags == 3) {
                long rawPTS = binToInt(binaryPESFields, position, position += PTSdtsLength);
                long rawDTS = binToInt(binaryPESFields, position, position += PTSdtsLength);
                
                PTS = parsePTSdts(nil,rawPTS);
                DTS = parsePTSdts(nil,rawDTS);

                tables.updateDTSpacketMap(DTS,packetIndex);
                tables.updateDTSpidMap(pid,DTS);
            }
            else {
                long rawPTS = binToInt(binaryPESFields, position, position += PTSdtsLength);
                PTS = parsePTSdts(rawPTS,nil);
            }
            tables.updatePTSpacketMap(PTS,packetIndex);
            tables.updatePTSpidMap(pid,PTS);
        }
        if (ESCRflag == 1) {
            ESCR = binToInt(binaryPESFields, position, position += ESCRlength);
        }
        if (ESrateFlag == 1) {
            ESrate = binToInt(binaryPESFields, position, position += ESrateLength);
        }
        if (DSMtrickModeFlag == 1) {
            DSMtrickMode = (int) binToInt(binaryPESFields, position, position += DSMtrickModeLength);
        }
        if (AdditionalCopyInfoFlag == 1) {
            AdditionalCopyInfo = (int) binToInt(binaryPESFields, position, position += AdditionalCopyInfoLength);
        }
        if (PEScrcFlag == 1) {
            PEScrc = binToInt(binaryPESFields, position, position += PEScrcLength);
        }
        if (PESextensionFlag == 1) {
            //TODO PES optional fields extension fields
        }

        return new PES(
                header,
                PTSdtsFlags,
                ESCRflag,
                ESrateFlag,
                DSMtrickModeFlag,
                AdditionalCopyInfoFlag,
                PEScrcFlag,
                PESextensionFlag,
                PESheaderDataLength,
                PTS,
                DTS,
                ESCR,
                ESrate,
                DSMtrickMode,
                AdditionalCopyInfo,
                PEScrc
        );
    }

}
