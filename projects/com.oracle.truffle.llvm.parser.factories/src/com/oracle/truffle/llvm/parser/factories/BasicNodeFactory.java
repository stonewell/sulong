/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to
 * endorse or promote products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.oracle.truffle.llvm.parser.factories;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;
import com.oracle.truffle.llvm.asm.amd64.InlineAssemblyParser;
import com.oracle.truffle.llvm.nodes.base.LLVMBasicBlockNode;
import com.oracle.truffle.llvm.nodes.base.LLVMFrameNuller;
import com.oracle.truffle.llvm.nodes.control.LLVMBrUnconditionalNode;
import com.oracle.truffle.llvm.nodes.control.LLVMConditionalBranchNode;
import com.oracle.truffle.llvm.nodes.control.LLVMDispatchBasicBlockNode;
import com.oracle.truffle.llvm.nodes.control.LLVMIndirectBranchNode;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVM80BitFloatRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMAddressRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMDoubleRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMFloatRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMFunctionRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI16RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI1RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI32RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI64RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMI8RetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMIVarBitRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMStructRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMVectorRetNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMRetNodeFactory.LLVMVoidReturnNodeGen;
import com.oracle.truffle.llvm.nodes.control.LLVMSwitchNode.LLVMSwitchNodeImpl;
import com.oracle.truffle.llvm.nodes.control.LLVMWritePhisNode;
import com.oracle.truffle.llvm.nodes.func.LLVMArgNodeGen;
import com.oracle.truffle.llvm.nodes.func.LLVMCallNode;
import com.oracle.truffle.llvm.nodes.func.LLVMFunctionStartNode;
import com.oracle.truffle.llvm.nodes.func.LLVMInlineAssemblyRootNode;
import com.oracle.truffle.llvm.nodes.func.LLVMInvokeNode;
import com.oracle.truffle.llvm.nodes.func.LLVMLandingpadNode;
import com.oracle.truffle.llvm.nodes.func.LLVMResumeNode;
import com.oracle.truffle.llvm.nodes.func.LLVMTypeIdForExceptionNode;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMCMathsIntrinsicsFactory;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMCMathsIntrinsicsFactory.LLVMFAbsNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.c.LLVMCMathsIntrinsicsFactory.LLVMPowNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMTruffleGetArgCountNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.interop.LLVMTruffleGetArgNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMAssumeNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapI16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapVI16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapVI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMByteSwapFactory.LLVMByteSwapVI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMExpectFactory.LLVMExpectI1NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMExpectFactory.LLVMExpectI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMExpectFactory.LLVMExpectI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMFrameAddressNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMI64ObjectSizeNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMInvariantEndNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMInvariantStartNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMLifetimeEndNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMLifetimeStartNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMMemCopyNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMMemMoveFactory.LLVMMemMoveI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMMemSetNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMNoOpNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMPrefetchNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMReturnAddressNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMStackRestoreNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMStackSaveNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.LLVMTrapNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmetic;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmeticFactory.GCCArithmeticNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmeticFactory.LLVMArithmeticWithOverflowAndCarryNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMArithmeticFactory.LLVMArithmeticWithOverflowNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.arith.LLVMComplexDivSC;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountLeadingZeroesNodeFactory.CountLeadingZeroesI8NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountSetBitsNodeFactory.CountSetBitsI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountSetBitsNodeFactory.CountSetBitsI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI16NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI32NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI64NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.bit.CountTrailingZeroesNodeFactory.CountTrailingZeroesI8NodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMDebugBuilder;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMDebugInitNodeFactory;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMDebugSimpleObjectBuilder;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMDebugWriteNodeFactory;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMFrameValueAccessImpl;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.debug.LLVMToDebugValueNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_64BitVACopyNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_64BitVAEnd;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_64VAStartNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_ConversionNodeFactory.LLVMX86_ConversionDoubleToIntNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_ConversionNodeFactory.LLVMX86_ConversionFloatToIntNodeGen;
import com.oracle.truffle.llvm.nodes.intrinsics.llvm.x86.LLVMX86_ConversionNodeFactory.LLVMX86_Pmovmskb128NodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMFunctionLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMFunctionLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVM80BitFloatLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMDoubleLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMFloatLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI16LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI1LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI32LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI64LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMI8LiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMIVarBitLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMManagedPointerLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMSimpleLiteralNode.LLVMNativePointerLiteralNode;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorAddressLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorDoubleLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorFloatLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorFunctionLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI16LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI1LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI32LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI64LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.literals.LLVMVectorLiteralNodeFactory.LLVMVectorI8LiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMGetStackSpaceInstruction.LLVMGetStackForConstInstruction;
import com.oracle.truffle.llvm.nodes.memory.LLVMGetStackSpaceInstructionFactory.LLVMAllocaConstInstructionNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMGetStackSpaceInstructionFactory.LLVMGetUniqueStackSpaceInstructionNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMGetStackSpaceInstructionFactory.LLVMAllocaInstructionNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMCompareExchangeNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMFenceNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMGetElementPtrNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMInsertValueNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMNativeVarargsAreaStackAllocationNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMStructByValueNodeGen;
import com.oracle.truffle.llvm.nodes.memory.LLVMVarArgCompoundAddressNodeGen;
import com.oracle.truffle.llvm.nodes.memory.NativeAllocateStringNodeGen;
import com.oracle.truffle.llvm.nodes.memory.NativeMemSetNodeGen;
import com.oracle.truffle.llvm.nodes.memory.NativeProfiledMemMoveNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVM80BitFloatArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMDoubleArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMFloatArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMFunctionArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI16ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI32ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI64ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMI8ArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMPointerArrayLiteralNode;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMPointerArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.literal.LLVMStructArrayLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNode.LLVMGlobalDirectLoadNode;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVM80BitFloatDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMFunctionDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMIVarBitDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMPointerDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDirectLoadNodeFactory.LLVMStructDirectLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMDoubleLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMFloatLoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI16LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI1LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI32LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI64LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMI8LoadNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadDoubleVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadFloatVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI16VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI1VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI32VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI64VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadI8VectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.load.LLVMLoadVectorNodeFactory.LLVMLoadPointerVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI16RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI1RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI32RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI64RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.rmw.LLVMI8RMWNodeFactory;
import com.oracle.truffle.llvm.nodes.memory.store.LLVM80BitFloatStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMDoubleStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMFloatStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMFunctionStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMGlobalVariableStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI16StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI1StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI32StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI64StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMI8StoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMIVarBitStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMPointerStoreNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMStoreVectorNodeGen;
import com.oracle.truffle.llvm.nodes.memory.store.LLVMStructStoreNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMAddNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMDivNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMMulNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMRemNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMSubNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMUDivNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMArithmeticNodeFactory.LLVMURemNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMEqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMNqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOeqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOgeNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOgtNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOleNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOltNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOneNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMOrdNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSgeNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSgtNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSleNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMSltNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUeqNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUgeNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUgtNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUleNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUltNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUneNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMCompareNodeFactory.LLVMUnoNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMAndNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMAshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMLshrNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMOrNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMShlNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMLogicNodeFactory.LLVMXorNodeGen;
import com.oracle.truffle.llvm.nodes.op.LLVMPointerCompareNode;
import com.oracle.truffle.llvm.nodes.others.LLVMAccessGlobalVariableStorageNode;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVM80BitFloatSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMDoubleSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMFloatSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMGenericSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI16SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI1SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI32SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI64SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMSelectNodeFactory.LLVMI8SelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMUnreachableNode;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMAddressProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMDoubleProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMFloatProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI16ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI1ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI32ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI64ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMValueProfilingNodeFactory.LLVMI8ProfiledValueNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMAddressVectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMDoubleVectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMFloatVectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI16VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI1VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI32VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI64VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.others.LLVMVectorSelectNodeFactory.LLVMI8VectorSelectNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVM80BitFloatReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMAddressReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMDoubleReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMFloatReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMFunctionReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI16ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI1ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI32ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI64ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMI8ReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadNodeFactory.LLVMIReadVarBitNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMAddressVectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMDoubleVectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMFloatVectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI16VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI1VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI32VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI64VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMReadVectorNodeFactory.LLVMI8VectorReadNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMSetInteropTypeNode;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNode;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWrite80BitFloatingNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteDoubleNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteFloatNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteFunctionNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI16NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI1NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI32NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI64NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteI8NodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteIVarBitNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWritePointerNodeGen;
import com.oracle.truffle.llvm.nodes.vars.LLVMWriteNodeFactory.LLVMWriteVectorNodeGen;
import com.oracle.truffle.llvm.nodes.vars.StructLiteralNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMAddressExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMDoubleExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMFloatExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI16ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI1ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI32ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI64ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMExtractElementNodeFactory.LLVMI8ExtractElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMDoubleInsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMFloatInsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI16InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI1InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI32InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI64InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMI8InsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMInsertElementNodeFactory.LLVMPointerInsertElementNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleDoubleVectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleFloatVectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleI16VectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleI32VectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleI64VectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShuffleI8VectorNodeGen;
import com.oracle.truffle.llvm.nodes.vector.LLVMShuffleVectorNodeFactory.LLVMShufflePointerVectorNodeGen;
import com.oracle.truffle.llvm.parser.GetStackSpaceFactory;
import com.oracle.truffle.llvm.parser.NodeFactory;
import com.oracle.truffle.llvm.parser.instructions.LLVMArithmeticInstructionType;
import com.oracle.truffle.llvm.parser.instructions.LLVMConversionType;
import com.oracle.truffle.llvm.parser.instructions.LLVMLogicalInstructionKind;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute;
import com.oracle.truffle.llvm.parser.model.attributes.Attribute.KnownAttribute;
import com.oracle.truffle.llvm.parser.model.attributes.AttributesGroup;
import com.oracle.truffle.llvm.parser.model.enums.CompareOperator;
import com.oracle.truffle.llvm.parser.model.enums.Flag;
import com.oracle.truffle.llvm.parser.model.enums.ReadModifyWriteOperator;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDeclaration;
import com.oracle.truffle.llvm.parser.model.functions.FunctionDefinition;
import com.oracle.truffle.llvm.runtime.LLVMContext;
import com.oracle.truffle.llvm.runtime.LLVMContext.ExternalLibrary;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor;
import com.oracle.truffle.llvm.runtime.LLVMFunctionDescriptor.LLVMIRFunction;
import com.oracle.truffle.llvm.runtime.LLVMIVarBit;
import com.oracle.truffle.llvm.runtime.LLVMLanguage;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException;
import com.oracle.truffle.llvm.runtime.LLVMUnsupportedException.UnsupportedReason;
import com.oracle.truffle.llvm.runtime.debug.scope.LLVMSourceLocation;
import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourcePointerType;
import com.oracle.truffle.llvm.runtime.debug.type.LLVMSourceType;
import com.oracle.truffle.llvm.runtime.debug.value.LLVMDebugObjectBuilder;
import com.oracle.truffle.llvm.runtime.debug.value.LLVMDebugValue;
import com.oracle.truffle.llvm.runtime.debug.value.LLVMFrameValueAccess;
import com.oracle.truffle.llvm.runtime.floating.LLVM80BitFloat;
import com.oracle.truffle.llvm.runtime.global.LLVMGlobal;
import com.oracle.truffle.llvm.runtime.interop.access.LLVMInteropType;
import com.oracle.truffle.llvm.runtime.memory.LLVMAllocateStringNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemMoveNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMMemSetNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMUniquesRegionAllocNode;
import com.oracle.truffle.llvm.runtime.memory.LLVMUniquesRegionAllocNodeGen;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack.UniquesRegion;
import com.oracle.truffle.llvm.runtime.memory.LLVMStack.UniquesRegion.UniqueSlot;
import com.oracle.truffle.llvm.runtime.memory.VarargsAreaStackAllocationNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMControlFlowNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMExpressionNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMLoadNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStatementNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMStoreNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMToNativeNode;
import com.oracle.truffle.llvm.runtime.nodes.api.LLVMTypesGen;
import com.oracle.truffle.llvm.runtime.pointer.LLVMManagedPointer;
import com.oracle.truffle.llvm.runtime.pointer.LLVMNativePointer;
import com.oracle.truffle.llvm.runtime.types.AggregateType;
import com.oracle.truffle.llvm.runtime.types.ArrayType;
import com.oracle.truffle.llvm.runtime.types.FunctionType;
import com.oracle.truffle.llvm.runtime.types.MetaType;
import com.oracle.truffle.llvm.runtime.types.PointerType;
import com.oracle.truffle.llvm.runtime.types.PrimitiveType;
import com.oracle.truffle.llvm.runtime.types.StructureType;
import com.oracle.truffle.llvm.runtime.types.Type;
import com.oracle.truffle.llvm.runtime.types.VariableBitWidthType;
import com.oracle.truffle.llvm.runtime.types.VectorType;
import com.oracle.truffle.llvm.runtime.types.VoidType;
import com.oracle.truffle.llvm.runtime.types.symbols.Symbol;

public class BasicNodeFactory implements NodeFactory {

    @Override
    public LLVMExpressionNode createInsertElement(Type resultType, LLVMExpressionNode vector, LLVMExpressionNode element,
                    LLVMExpressionNode index) {
        VectorType resultType1 = (VectorType) resultType;
        if (resultType1.getElementType() instanceof PrimitiveType) {
            switch (((PrimitiveType) resultType1.getElementType()).getPrimitiveKind()) {
                case I1:
                    return LLVMI1InsertElementNodeGen.create(vector, element, index);
                case I8:
                    return LLVMI8InsertElementNodeGen.create(vector, element, index);
                case I16:
                    return LLVMI16InsertElementNodeGen.create(vector, element, index);
                case I32:
                    return LLVMI32InsertElementNodeGen.create(vector, element, index);
                case I64:
                    return LLVMI64InsertElementNodeGen.create(vector, element, index);
                case FLOAT:
                    return LLVMFloatInsertElementNodeGen.create(vector, element, index);
                case DOUBLE:
                    return LLVMDoubleInsertElementNodeGen.create(vector, element, index);
                default:
                    throw new AssertionError("vector type " + resultType1 + "  not supported!");
            }
        } else if (resultType1.getElementType() instanceof PointerType) {
            return LLVMPointerInsertElementNodeGen.create(vector, element, index);
        }
        throw new AssertionError(resultType1);
    }

    @Override
    public LLVMExpressionNode createExtractElement(Type resultType, LLVMExpressionNode vector, LLVMExpressionNode index) {
        if (resultType instanceof PrimitiveType) {
            PrimitiveType resultType1 = (PrimitiveType) resultType;
            switch (resultType1.getPrimitiveKind()) {
                case I1:
                    return LLVMI1ExtractElementNodeGen.create(vector, index);
                case I8:
                    return LLVMI8ExtractElementNodeGen.create(vector, index);
                case I16:
                    return LLVMI16ExtractElementNodeGen.create(vector, index);
                case I32:
                    return LLVMI32ExtractElementNodeGen.create(vector, index);
                case I64:
                    return LLVMI64ExtractElementNodeGen.create(vector, index);
                case FLOAT:
                    return LLVMFloatExtractElementNodeGen.create(vector, index);
                case DOUBLE:
                    return LLVMDoubleExtractElementNodeGen.create(vector, index);
                default:
                    throw new AssertionError(resultType1 + " not supported!");
            }
        } else if (resultType instanceof PointerType) {
            return LLVMAddressExtractElementNodeGen.create(vector, index);
        } else {
            throw new AssertionError(resultType + " not supported!");
        }
    }

    @Override
    public LLVMExpressionNode createShuffleVector(Type llvmType, LLVMExpressionNode vector1, LLVMExpressionNode vector2,
                    LLVMExpressionNode mask) {
        VectorType resultType = (VectorType) llvmType;
        if (resultType.getElementType() instanceof PrimitiveType) {
            switch (((PrimitiveType) resultType.getElementType()).getPrimitiveKind()) {
                case I8:
                    return LLVMShuffleI8VectorNodeGen.create(vector1, vector2, mask);
                case I16:
                    return LLVMShuffleI16VectorNodeGen.create(vector1, vector2, mask);
                case I32:
                    return LLVMShuffleI32VectorNodeGen.create(vector1, vector2, mask);
                case I64:
                    return LLVMShuffleI64VectorNodeGen.create(vector1, vector2, mask);
                case FLOAT:
                    return LLVMShuffleFloatVectorNodeGen.create(vector1, vector2, mask);
                case DOUBLE:
                    return LLVMShuffleDoubleVectorNodeGen.create(vector1, vector2, mask);
                default:
                    throw new AssertionError(resultType);
            }
        } else if (resultType.getElementType() instanceof PointerType) {
            return LLVMShufflePointerVectorNodeGen.create(vector1, vector2, mask);
        }
        throw new AssertionError(resultType);
    }

    @Override
    public LLVMExpressionNode createLoad(Type resolvedResultType, LLVMExpressionNode loadTarget) {
        if (resolvedResultType instanceof VectorType) {
            return createLoadVector((VectorType) resolvedResultType, loadTarget, ((VectorType) resolvedResultType).getNumberOfElements());
        } else {
            int bits = resolvedResultType instanceof VariableBitWidthType
                            ? resolvedResultType.getBitSize()
                            : 0;
            return createLoad(resolvedResultType, loadTarget, bits);
        }
    }

    private static LLVMLoadNode createLoadVector(VectorType resultType, LLVMExpressionNode loadTarget, int size) {
        Type elemType = resultType.getElementType();
        if (elemType instanceof PrimitiveType) {

            switch (((PrimitiveType) elemType).getPrimitiveKind()) {
                case I1:
                    return LLVMLoadI1VectorNodeGen.create(loadTarget, size);
                case I8:
                    return LLVMLoadI8VectorNodeGen.create(loadTarget, size);
                case I16:
                    return LLVMLoadI16VectorNodeGen.create(loadTarget, size);
                case I32:
                    return LLVMLoadI32VectorNodeGen.create(loadTarget, size);
                case I64:
                    return LLVMLoadI64VectorNodeGen.create(loadTarget, size);
                case FLOAT:
                    return LLVMLoadFloatVectorNodeGen.create(loadTarget, size);
                case DOUBLE:
                    return LLVMLoadDoubleVectorNodeGen.create(loadTarget, size);
                default:
                    throw new AssertionError(elemType + " vectors not supported");
            }
        } else if (elemType instanceof PointerType) {
            return LLVMLoadPointerVectorNodeGen.create(loadTarget, size);
        } else {
            throw new AssertionError(elemType + " vectors not supported");
        }
    }

    @Override
    public LLVMStatementNode createStore(LLVMContext context, LLVMExpressionNode pointerNode, LLVMExpressionNode valueNode, Type type, LLVMSourceLocation source) {
        return createStore(pointerNode, valueNode, type, context.getByteSize(type), source);
    }

    @Override
    public LLVMExpressionNode createReadModifyWrite(ReadModifyWriteOperator operator, LLVMExpressionNode pointerNode, LLVMExpressionNode valueNode, Type type) {
        if (type instanceof PrimitiveType) {
            switch (operator) {
                case XCHG:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWXchgNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWXchgNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case ADD:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWAddNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWAddNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWAddNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWAddNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWAddNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case SUB:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWSubNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWSubNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWSubNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWSubNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWSubNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case AND:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWAndNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWAndNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWAndNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWAndNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWAndNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case NAND:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWNandNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWNandNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWNandNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWNandNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWNandNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case OR:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWOrNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWOrNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWOrNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWOrNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWOrNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case XOR:
                    switch (((PrimitiveType) type).getPrimitiveKind()) {
                        case I1:
                            return LLVMI1RMWNodeFactory.LLVMI1RMWXorNodeGen.create(pointerNode, valueNode);
                        case I8:
                            return LLVMI8RMWNodeFactory.LLVMI8RMWXorNodeGen.create(pointerNode, valueNode);
                        case I16:
                            return LLVMI16RMWNodeFactory.LLVMI16RMWXorNodeGen.create(pointerNode, valueNode);
                        case I32:
                            return LLVMI32RMWNodeFactory.LLVMI32RMWXorNodeGen.create(pointerNode, valueNode);
                        case I64:
                            return LLVMI64RMWNodeFactory.LLVMI64RMWXorNodeGen.create(pointerNode, valueNode);
                        default:
                            throw new AssertionError(type);
                    }
                case MAX:
                case MIN:
                case UMAX:
                case UMIN:
                default:
                    throw new AssertionError(operator);
            }
        } else {
            throw new AssertionError(type);
        }
    }

    @Override
    public LLVMStatementNode createFence() {
        return LLVMFenceNodeGen.create();
    }

    @Override
    public LLVMExpressionNode createLogicalOperation(LLVMExpressionNode left, LLVMExpressionNode right, LLVMLogicalInstructionKind type, Type llvmType, Flag[] flags) {
        switch (type) {
            case AND:
                return LLVMAndNodeGen.create(left, right);
            case ARITHMETIC_SHIFT_RIGHT:
                return LLVMAshrNodeGen.create(left, right);
            case LOGICAL_SHIFT_RIGHT:
                return LLVMLshrNodeGen.create(left, right);
            case OR:
                return LLVMOrNodeGen.create(left, right);
            case SHIFT_LEFT:
                return LLVMShlNodeGen.create(left, right);
            case XOR:
                return LLVMXorNodeGen.create(left, right);
            default:
                throw new AssertionError(llvmType);
        }
    }

    @Override
    public LLVMExpressionNode createSimpleConstantNoArray(LLVMContext context, Object constant, Type type) {
        if (Type.isFunctionOrFunctionPointer(type)) {
            if (constant == null) {
                LLVMFunctionDescriptor functionDescriptor = context.getFunctionDescriptor(LLVMNativePointer.createNull());
                return LLVMFunctionLiteralNodeGen.create(functionDescriptor);
            } else {
                throw new AssertionError("Not a Simple Constant: " + constant);
            }
        } else if (type instanceof VariableBitWidthType) {
            Number c = (Number) constant;
            if (type.getBitSize() <= Long.SIZE) {
                return new LLVMIVarBitLiteralNode(LLVMIVarBit.fromLong(type.getBitSize(), c.longValue()));
            } else {
                return new LLVMIVarBitLiteralNode(LLVMIVarBit.fromBigInteger(type.getBitSize(), (BigInteger) c));
            }
        } else if (type instanceof PointerType) {
            if (constant == null) {
                return new LLVMNativePointerLiteralNode(LLVMNativePointer.create(0));
            } else {
                throw new AssertionError("Not a Simple Constant: " + constant);
            }
        } else if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return new LLVMI1LiteralNode((boolean) constant);
                case I8:
                    return new LLVMI8LiteralNode((byte) constant);
                case I16:
                    return new LLVMI16LiteralNode((short) constant);
                case I32:
                    return new LLVMI32LiteralNode((int) constant);
                case FLOAT:
                    return new LLVMFloatLiteralNode((float) constant);
                case DOUBLE:
                    return new LLVMDoubleLiteralNode((double) constant);
                case X86_FP80:
                    if (constant == null) {
                        return new LLVM80BitFloatLiteralNode(LLVM80BitFloat.fromLong(0));
                    } else {
                        return new LLVM80BitFloatLiteralNode(LLVM80BitFloat.fromBytesBigEndian((byte[]) constant));
                    }
                case I64:
                    return new LLVMI64LiteralNode((long) constant);
                default:
                    throw new AssertionError(type);
            }
        } else if (type == MetaType.DEBUG) {
            return new LLVMNativePointerLiteralNode(LLVMNativePointer.createNull());
        } else {
            throw new AssertionError(type);
        }
    }

    @Override
    public LLVMExpressionNode createVectorLiteralNode(List<LLVMExpressionNode> listValues, Type type) {
        LLVMExpressionNode[] vals = listValues.toArray(new LLVMExpressionNode[listValues.size()]);
        Type llvmType = ((VectorType) type).getElementType();
        if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I1:
                    return LLVMVectorI1LiteralNodeGen.create(vals);
                case I8:
                    return LLVMVectorI8LiteralNodeGen.create(vals);
                case I16:
                    return LLVMVectorI16LiteralNodeGen.create(vals);
                case I32:
                    return LLVMVectorI32LiteralNodeGen.create(vals);
                case I64:
                    return LLVMVectorI64LiteralNodeGen.create(vals);
                case FLOAT:
                    return LLVMVectorFloatLiteralNodeGen.create(vals);
                case DOUBLE:
                    return LLVMVectorDoubleLiteralNodeGen.create(vals);
                default:
                    throw new AssertionError();
            }
        } else if (llvmType instanceof PointerType) {
            if (((PointerType) llvmType).getPointeeType() instanceof FunctionType) {
                return LLVMVectorFunctionLiteralNodeGen.create(vals);
            } else {
                return LLVMVectorAddressLiteralNodeGen.create(vals);
            }
        } else {
            throw new AssertionError(llvmType + " not yet supported");
        }
    }

    @Override
    public LLVMFrameNuller createFrameNuller(FrameSlot slot) {
        return new LLVMFrameNuller(slot);
    }

    @Override
    public LLVMControlFlowNode createRetVoid(LLVMSourceLocation source) {
        return LLVMVoidReturnNodeGen.create(source);
    }

    @Override
    public LLVMControlFlowNode createNonVoidRet(LLVMContext context, LLVMExpressionNode retValue, Type type, LLVMSourceLocation source) {
        if (retValue == null) {
            throw new AssertionError();
        }
        if (type instanceof VectorType) {
            return LLVMVectorRetNodeGen.create(source, retValue);
        } else if (type instanceof VariableBitWidthType) {
            return LLVMIVarBitRetNodeGen.create(source, retValue);
        } else if (Type.isFunctionOrFunctionPointer(type)) {
            return LLVMFunctionRetNodeGen.create(source, retValue);
        } else if (type instanceof PointerType) {
            return LLVMAddressRetNodeGen.create(source, retValue);
        } else if (type instanceof StructureType) {
            int size = context.getByteSize(type);
            return LLVMStructRetNodeGen.create(source, createMemMove(), retValue, size);
        } else if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return LLVMI1RetNodeGen.create(source, retValue);
                case I8:
                    return LLVMI8RetNodeGen.create(source, retValue);
                case I16:
                    return LLVMI16RetNodeGen.create(source, retValue);
                case I32:
                    return LLVMI32RetNodeGen.create(source, retValue);
                case I64:
                    return LLVMI64RetNodeGen.create(source, retValue);
                case FLOAT:
                    return LLVMFloatRetNodeGen.create(source, retValue);
                case DOUBLE:
                    return LLVMDoubleRetNodeGen.create(source, retValue);
                case X86_FP80:
                    return LLVM80BitFloatRetNodeGen.create(source, retValue);
                default:
                    throw new AssertionError(type);
            }
        }
        throw new AssertionError(type);
    }

    @Override
    public LLVMExpressionNode createFunctionArgNode(int argIndex, Type paramType) {
        if (argIndex < 0) {
            throw new AssertionError();
        }
        LLVMExpressionNode argNode = LLVMArgNodeGen.create(argIndex);
        if (argIndex < LLVMCallNode.USER_ARGUMENT_OFFSET) {
            // Do not profile the stackpointer
            return argNode;
        }
        if (paramType instanceof PrimitiveType) {
            switch (((PrimitiveType) paramType).getPrimitiveKind()) {
                case I8:
                    return LLVMI8ProfiledValueNodeGen.create(argNode);
                case I32:
                    return LLVMI32ProfiledValueNodeGen.create(argNode);
                case I64:
                    return LLVMI64ProfiledValueNodeGen.create(argNode);
                case FLOAT:
                    return LLVMFloatProfiledValueNodeGen.create(argNode);
                case DOUBLE:
                    return LLVMDoubleProfiledValueNodeGen.create(argNode);
                case I1:
                    return LLVMI1ProfiledValueNodeGen.create(argNode);
                case I16:
                    return LLVMI16ProfiledValueNodeGen.create(argNode);
                default:
                    return argNode;
            }
        } else if (paramType instanceof PointerType) {
            return LLVMAddressProfiledValueNodeGen.create(argNode);
        } else {
            return argNode;
        }
    }

    @Override
    public LLVMExpressionNode createFunctionCall(LLVMExpressionNode functionNode, LLVMExpressionNode[] argNodes, FunctionType type, LLVMSourceLocation sourceSection) {
        LLVMExpressionNode callNode = new LLVMCallNode(type, functionNode, argNodes, sourceSection);
        if (type.getReturnType() instanceof PrimitiveType) {
            switch (((PrimitiveType) type.getReturnType()).getPrimitiveKind()) {
                case I8:
                    return LLVMI8ProfiledValueNodeGen.create(callNode);
                case I32:
                    return LLVMI32ProfiledValueNodeGen.create(callNode);
                case I64:
                    return LLVMI64ProfiledValueNodeGen.create(callNode);
                case FLOAT:
                    return LLVMFloatProfiledValueNodeGen.create(callNode);
                case DOUBLE:
                    return LLVMDoubleProfiledValueNodeGen.create(callNode);
                case I1:
                    return LLVMI1ProfiledValueNodeGen.create(callNode);
                case I16:
                    return LLVMI16ProfiledValueNodeGen.create(callNode);
                default:
                    return callNode;
            }
        } else if (type.getReturnType() instanceof PointerType) {
            return LLVMAddressProfiledValueNodeGen.create(callNode);
        } else {
            return callNode;
        }
    }

    @Override
    public LLVMExpressionNode createFrameRead(Type llvmType, FrameSlot frameSlot) {
        if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I1:
                    return LLVMI1ReadNodeGen.create(frameSlot);
                case I8:
                    return LLVMI8ReadNodeGen.create(frameSlot);
                case I16:
                    return LLVMI16ReadNodeGen.create(frameSlot);
                case I32:
                    return LLVMI32ReadNodeGen.create(frameSlot);
                case I64:
                    return LLVMI64ReadNodeGen.create(frameSlot);
                case FLOAT:
                    return LLVMFloatReadNodeGen.create(frameSlot);
                case DOUBLE:
                    return LLVMDoubleReadNodeGen.create(frameSlot);
                case X86_FP80:
                    return LLVM80BitFloatReadNodeGen.create(frameSlot);
            }
        } else if (llvmType instanceof VectorType) {
            Type elemType = ((VectorType) llvmType).getElementType();
            if (elemType instanceof PrimitiveType) {
                switch (((PrimitiveType) elemType).getPrimitiveKind()) {
                    case I1:
                        return LLVMI1VectorReadNodeGen.create(frameSlot);
                    case I8:
                        return LLVMI8VectorReadNodeGen.create(frameSlot);
                    case I16:
                        return LLVMI16VectorReadNodeGen.create(frameSlot);
                    case I32:
                        return LLVMI32VectorReadNodeGen.create(frameSlot);
                    case I64:
                        return LLVMI64VectorReadNodeGen.create(frameSlot);
                    case FLOAT:
                        return LLVMFloatVectorReadNodeGen.create(frameSlot);
                    case DOUBLE:
                        return LLVMDoubleVectorReadNodeGen.create(frameSlot);
                }
            } else if (elemType instanceof PointerType) {
                return LLVMAddressVectorReadNodeGen.create(frameSlot);
            }
        } else if (llvmType instanceof VariableBitWidthType) {
            return LLVMIReadVarBitNodeGen.create(frameSlot);
        } else if (Type.isFunctionOrFunctionPointer(llvmType)) {
            return LLVMFunctionReadNodeGen.create(frameSlot);
        } else if (llvmType instanceof PointerType) {
            return LLVMAddressReadNodeGen.create(frameSlot);
        } else if (llvmType instanceof StructureType || llvmType instanceof ArrayType) {
            return LLVMAddressReadNodeGen.create(frameSlot);
        } else if (llvmType instanceof VoidType) {
            throw new LLVMUnsupportedException(UnsupportedReason.PARSER_ERROR_VOID_SLOT);
        } else if (llvmType == MetaType.DEBUG) {
            return LLVMReadNodeFactory.LLVMDebugReadNodeGen.create(frameSlot);
        }
        throw new AssertionError(llvmType + " for " + frameSlot.getIdentifier());
    }

    @Override
    public LLVMWriteNode createFrameWrite(Type llvmType, LLVMExpressionNode result, FrameSlot slot, LLVMSourceLocation sourceSection) {
        if (llvmType instanceof VectorType) {
            return LLVMWriteVectorNodeGen.create(result, slot, sourceSection);
        } else if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I1:
                    return LLVMWriteI1NodeGen.create(result, slot, sourceSection);
                case I8:
                    return LLVMWriteI8NodeGen.create(result, slot, sourceSection);
                case I16:
                    return LLVMWriteI16NodeGen.create(result, slot, sourceSection);
                case I32:
                    return LLVMWriteI32NodeGen.create(result, slot, sourceSection);
                case I64:
                    return LLVMWriteI64NodeGen.create(result, slot, sourceSection);
                case FLOAT:
                    return LLVMWriteFloatNodeGen.create(result, slot, sourceSection);
                case DOUBLE:
                    return LLVMWriteDoubleNodeGen.create(result, slot, sourceSection);
                case X86_FP80:
                    return LLVMWrite80BitFloatingNodeGen.create(result, slot, sourceSection);
                default:
                    throw new AssertionError(llvmType);
            }
        } else if (llvmType instanceof VariableBitWidthType) {
            return LLVMWriteIVarBitNodeGen.create(result, slot, sourceSection);
        } else if (Type.isFunctionOrFunctionPointer(llvmType)) {
            return LLVMWriteFunctionNodeGen.create(result, slot, sourceSection);
        } else if (llvmType instanceof PointerType) {
            return LLVMWritePointerNodeGen.create(result, slot, sourceSection);
        } else if (llvmType instanceof StructureType || llvmType instanceof ArrayType) {
            return LLVMWritePointerNodeGen.create(result, slot, sourceSection);
        }
        throw new AssertionError(llvmType);
    }

    @Override
    public LLVMExpressionNode createComparison(CompareOperator operator, Type type, LLVMExpressionNode lhs, LLVMExpressionNode rhs) {
        if (Type.isFunctionOrFunctionPointer(type) || type instanceof PointerType) {
            switch (operator) {
                case INT_EQUAL:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.EQ, lhs, rhs);
                case INT_NOT_EQUAL:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.NEQ, lhs, rhs);
                case INT_UNSIGNED_GREATER_THAN:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.UGT, lhs, rhs);
                case INT_UNSIGNED_GREATER_OR_EQUAL:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.UGE, lhs, rhs);
                case INT_UNSIGNED_LESS_THAN:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.ULT, lhs, rhs);
                case INT_UNSIGNED_LESS_OR_EQUAL:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.ULE, lhs, rhs);
                case INT_SIGNED_GREATER_THAN:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.SGT, lhs, rhs);
                case INT_SIGNED_GREATER_OR_EQUAL:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.SGE, lhs, rhs);
                case INT_SIGNED_LESS_THAN:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.SLT, lhs, rhs);
                case INT_SIGNED_LESS_OR_EQUAL:
                    return LLVMPointerCompareNode.create(LLVMPointerCompareNode.Kind.SLE, lhs, rhs);
                default:
                    throw new AssertionError(operator);
            }
        }
        switch (operator) {
            case FP_FALSE:
                return new LLVMI1LiteralNode(false);
            case FP_ORDERED_EQUAL:
                return LLVMOeqNodeGen.create(lhs, rhs);
            case FP_ORDERED_GREATER_THAN:
                return LLVMOgtNodeGen.create(lhs, rhs);
            case FP_ORDERED_GREATER_OR_EQUAL:
                return LLVMOgeNodeGen.create(lhs, rhs);
            case FP_ORDERED_LESS_THAN:
                return LLVMOltNodeGen.create(lhs, rhs);
            case FP_ORDERED_LESS_OR_EQUAL:
                return LLVMOleNodeGen.create(lhs, rhs);
            case FP_ORDERED_NOT_EQUAL:
                return LLVMOneNodeGen.create(lhs, rhs);
            case FP_ORDERED:
                return LLVMOrdNodeGen.create(lhs, rhs);
            case FP_UNORDERED:
                return LLVMUnoNodeGen.create(lhs, rhs);
            case FP_UNORDERED_EQUAL:
                return LLVMUeqNodeGen.create(lhs, rhs);
            case FP_UNORDERED_GREATER_THAN:
                return LLVMUgtNodeGen.create(lhs, rhs);
            case FP_UNORDERED_GREATER_OR_EQUAL:
                return LLVMUgeNodeGen.create(lhs, rhs);
            case FP_UNORDERED_LESS_THAN:
                return LLVMUltNodeGen.create(lhs, rhs);
            case FP_UNORDERED_LESS_OR_EQUAL:
                return LLVMUleNodeGen.create(lhs, rhs);
            case FP_UNORDERED_NOT_EQUAL:
                return LLVMUneNodeGen.create(lhs, rhs);
            case FP_TRUE:
                return new LLVMI1LiteralNode(true);
            case INT_EQUAL:
                return LLVMEqNodeGen.create(lhs, rhs);
            case INT_NOT_EQUAL:
                return LLVMNqNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_GREATER_THAN:
                return LLVMUgtNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_GREATER_OR_EQUAL:
                return LLVMUgeNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_LESS_THAN:
                return LLVMUltNodeGen.create(lhs, rhs);
            case INT_UNSIGNED_LESS_OR_EQUAL:
                return LLVMUleNodeGen.create(lhs, rhs);
            case INT_SIGNED_GREATER_THAN:
                return LLVMSgtNodeGen.create(lhs, rhs);
            case INT_SIGNED_GREATER_OR_EQUAL:
                return LLVMSgeNodeGen.create(lhs, rhs);
            case INT_SIGNED_LESS_THAN:
                return LLVMSltNodeGen.create(lhs, rhs);
            case INT_SIGNED_LESS_OR_EQUAL:
                return LLVMSleNodeGen.create(lhs, rhs);
            default:
                throw new RuntimeException("Missed a compare operator");
        }
    }

    @Override
    public LLVMExpressionNode createCast(LLVMExpressionNode fromNode, Type targetType, Type fromType, LLVMConversionType type) {
        return LLVMCastsFactory.cast(fromNode, targetType, fromType, type);
    }

    @Override
    public LLVMExpressionNode createArithmeticOperation(LLVMExpressionNode left, LLVMExpressionNode right, LLVMArithmeticInstructionType type, Type llvmType, Flag[] flags) {
        switch (type) {
            case ADDITION:
                return LLVMAddNodeGen.create(left, right);
            case SUBTRACTION:
                return LLVMSubNodeGen.create(left, right);
            case MULTIPLICATION:
                return LLVMMulNodeGen.create(left, right);
            case DIVISION:
                return LLVMDivNodeGen.create(left, right);
            case REMAINDER:
                return LLVMRemNodeGen.create(left, right);
            case UNSIGNED_REMAINDER:
                return LLVMURemNodeGen.create(left, right);
            case UNSIGNED_DIVISION:
                return LLVMUDivNodeGen.create(left, right);
            default:
                throw new AssertionError(type);
        }
    }

    @Override
    public LLVMLoadNode createExtractValue(Type type, LLVMExpressionNode targetAddress) {
        if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return LLVMI1LoadNodeGen.create(targetAddress);
                case I8:
                    return LLVMI8LoadNodeGen.create(targetAddress);
                case I16:
                    return LLVMI16LoadNodeGen.create(targetAddress);
                case I32:
                    return LLVMI32LoadNodeGen.create(targetAddress);
                case I64:
                    return LLVMI64LoadNodeGen.create(targetAddress);
                case FLOAT:
                    return LLVMFloatLoadNodeGen.create(targetAddress);
                case DOUBLE:
                    return LLVMDoubleLoadNodeGen.create(targetAddress);
                case X86_FP80:
                    return LLVM80BitFloatDirectLoadNodeGen.create(targetAddress);
                default:
                    throw new AssertionError(type);
            }
        } else if (type instanceof PointerType || type instanceof StructureType || type instanceof ArrayType) {
            return LLVMPointerDirectLoadNodeGen.create(targetAddress);
        } else {
            throw new AssertionError(type);
        }
    }

    @Override
    public LLVMExpressionNode createTypedElementPointer(LLVMExpressionNode aggregateAddress, LLVMExpressionNode index, long indexedTypeLength, Type targetType) {
        return LLVMGetElementPtrNodeGen.create(aggregateAddress, index, indexedTypeLength);
    }

    @Override
    public LLVMExpressionNode createSelect(Type type, LLVMExpressionNode condition, LLVMExpressionNode trueValue, LLVMExpressionNode falseValue) {
        if (type instanceof VectorType) {
            final Type elementType = ((VectorType) type).getElementType();
            if (elementType == PrimitiveType.I1) {
                return LLVMI1VectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (elementType == PrimitiveType.I8) {
                return LLVMI8VectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (elementType == PrimitiveType.I16) {
                return LLVMI16VectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (elementType == PrimitiveType.I32) {
                return LLVMI32VectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (elementType == PrimitiveType.I64) {
                return LLVMI64VectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (elementType == PrimitiveType.FLOAT) {
                return LLVMFloatVectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (elementType == PrimitiveType.DOUBLE) {
                return LLVMDoubleVectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else if (elementType instanceof PointerType) {
                return LLVMAddressVectorSelectNodeGen.create(condition, trueValue, falseValue);
            } else {
                throw new AssertionError("Cannot create vector select for type: " + type);
            }
        } else if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return LLVMI1SelectNodeGen.create(condition, trueValue, falseValue);
                case I8:
                    return LLVMI8SelectNodeGen.create(condition, trueValue, falseValue);
                case I16:
                    return LLVMI16SelectNodeGen.create(condition, trueValue, falseValue);
                case I32:
                    return LLVMI32SelectNodeGen.create(condition, trueValue, falseValue);
                case I64:
                    return LLVMI64SelectNodeGen.create(condition, trueValue, falseValue);
                case FLOAT:
                    return LLVMFloatSelectNodeGen.create(condition, trueValue, falseValue);
                case DOUBLE:
                    return LLVMDoubleSelectNodeGen.create(condition, trueValue, falseValue);
                case X86_FP80:
                    return LLVM80BitFloatSelectNodeGen.create(condition, trueValue, falseValue);
            }
        }
        return LLVMGenericSelectNodeGen.create(condition, trueValue, falseValue);
    }

    @Override
    public LLVMExpressionNode createZeroVectorInitializer(int nrElements, VectorType llvmType) {
        Type llvmType1 = llvmType.getElementType();
        if (llvmType1 instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType1).getPrimitiveKind()) {
                case I1:
                    LLVMExpressionNode[] i1Vals = createI1LiteralNodes(nrElements, false);
                    return LLVMVectorI1LiteralNodeGen.create(i1Vals);
                case I8:
                    LLVMExpressionNode[] i8Vals = createI8LiteralNodes(nrElements, (byte) 0);
                    return LLVMVectorI8LiteralNodeGen.create(i8Vals);
                case I16:
                    LLVMExpressionNode[] i16Vals = createI16LiteralNodes(nrElements, (short) 0);
                    return LLVMVectorI16LiteralNodeGen.create(i16Vals);
                case I32:
                    LLVMExpressionNode[] i32Vals = createI32LiteralNodes(nrElements, 0);
                    return LLVMVectorI32LiteralNodeGen.create(i32Vals);
                case I64:
                    LLVMExpressionNode[] i64Vals = createI64LiteralNodes(nrElements, 0);
                    return LLVMVectorI64LiteralNodeGen.create(i64Vals);
                case FLOAT:
                    LLVMExpressionNode[] floatVals = createFloatLiteralNodes(nrElements, 0.0f);
                    return LLVMVectorFloatLiteralNodeGen.create(floatVals);
                case DOUBLE:
                    LLVMExpressionNode[] doubleVals = createDoubleLiteralNodes(nrElements, 0.0f);
                    return LLVMVectorDoubleLiteralNodeGen.create(doubleVals);
                default:
                    throw new AssertionError(llvmType1);
            }
        } else if (llvmType1 instanceof PointerType) {
            LLVMExpressionNode[] addressVals = createNullAddressLiteralNodes(nrElements);
            return LLVMVectorAddressLiteralNodeGen.create(addressVals);
        } else {
            throw new AssertionError(llvmType1 + " not yet supported");
        }
    }

    @Override
    public LLVMExpressionNode createLiteral(Object value, Type type) {
        if (Type.isFunctionOrFunctionPointer(type)) {
            return LLVMFunctionLiteralNodeGen.create((LLVMFunctionDescriptor) value);
        } else if (type instanceof PointerType) {
            if (LLVMNativePointer.isInstance(value)) {
                return new LLVMNativePointerLiteralNode(LLVMNativePointer.cast(value));
            } else if (LLVMManagedPointer.isInstance(value)) {
                return new LLVMManagedPointerLiteralNode(LLVMManagedPointer.cast(value));
            } else if (value instanceof LLVMGlobal) {
                return new LLVMAccessGlobalVariableStorageNode((LLVMGlobal) value);
            } else {
                throw new AssertionError(value.getClass());
            }
        } else if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return new LLVMI1LiteralNode((boolean) value);
                case I8:
                    return new LLVMI8LiteralNode((byte) value);
                case I16:
                    return new LLVMI16LiteralNode((short) value);
                case I32:
                    return new LLVMI32LiteralNode((int) value);
                case I64:
                    return new LLVMI64LiteralNode((long) value);
                case FLOAT:
                    return new LLVMFloatLiteralNode((float) value);
                case DOUBLE:
                    return new LLVMDoubleLiteralNode((double) value);
                default:
                    throw new AssertionError(value + " " + type);
            }
        }
        throw new AssertionError(value + " " + type);
    }

    @Override
    public LLVMControlFlowNode createUnreachableNode() {
        return new LLVMUnreachableNode();
    }

    @Override
    public LLVMControlFlowNode createIndirectBranch(LLVMExpressionNode value, int[] labelTargets, LLVMStatementNode[] phiWrites, LLVMSourceLocation source) {
        return LLVMIndirectBranchNode.create(new LLVMIndirectBranchNode.LLVMBasicBranchAddressNode(value), labelTargets, phiWrites, source);
    }

    @Override
    public LLVMControlFlowNode createSwitch(LLVMExpressionNode cond, int[] successors, LLVMExpressionNode[] cases, Type llvmType, LLVMStatementNode[] phiWriteNodes, LLVMSourceLocation source) {
        LLVMExpressionNode[] caseNodes = Arrays.copyOf(cases, cases.length, LLVMExpressionNode[].class);
        return new LLVMSwitchNodeImpl(successors, phiWriteNodes, cond, caseNodes, source);
    }

    @Override
    public LLVMControlFlowNode createConditionalBranch(int trueIndex, int falseIndex, LLVMExpressionNode conditionNode, LLVMStatementNode truePhiWriteNodes,
                    LLVMStatementNode falsePhiWriteNodes, LLVMSourceLocation sourceSection) {
        return LLVMConditionalBranchNode.create(trueIndex, falseIndex, truePhiWriteNodes, falsePhiWriteNodes, conditionNode, sourceSection);
    }

    @Override
    public LLVMControlFlowNode createUnconditionalBranch(int unconditionalIndex, LLVMStatementNode phiWrites, LLVMSourceLocation source) {
        return LLVMBrUnconditionalNode.create(unconditionalIndex, phiWrites, source);
    }

    @Override
    public LLVMExpressionNode createArrayLiteral(LLVMContext context, List<LLVMExpressionNode> arrayValues, ArrayType arrayType, GetStackSpaceFactory arrayGetStackSpaceFactory) {
        assert arrayType.getNumberOfElements() == arrayValues.size();
        LLVMExpressionNode arrayGetStackSpace = arrayGetStackSpaceFactory.createGetStackSpace(this, context, arrayType);
        int nrElements = arrayValues.size();
        Type elementType = arrayType.getElementType();
        int elementSize = context.getByteSize(elementType);
        if (elementSize == 0) {
            throw new AssertionError(elementType + " has size of 0!");
        }
        if (elementType instanceof PrimitiveType) {
            switch (((PrimitiveType) elementType).getPrimitiveKind()) {
                case I8:
                    return LLVMI8ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
                case I16:
                    return LLVMI16ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
                case I32:
                    return LLVMI32ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
                case I64:
                    return LLVMI64ArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
                case FLOAT:
                    return LLVMFloatArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
                case DOUBLE:
                    return LLVMDoubleArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
                case X86_FP80:
                    return LLVM80BitFloatArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
                default:
                    throw new AssertionError(elementType);
            }
        } else if (Type.isFunctionOrFunctionPointer(elementType)) {
            return LLVMFunctionArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
        } else if (elementType instanceof PointerType) {
            return LLVMPointerArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), elementSize, arrayGetStackSpace);
        } else if (elementType instanceof ArrayType || elementType instanceof StructureType) {
            return LLVMStructArrayLiteralNodeGen.create(arrayValues.toArray(new LLVMExpressionNode[nrElements]), createMemMove(), elementSize, arrayGetStackSpace);
        }
        throw new AssertionError(elementType);
    }

    @Override
    public LLVMExpressionNode createAlloca(LLVMContext context, Type type) {
        int alignment = context.getByteAlignment(type);
        int byteSize = context.getByteSize(type);
        LLVMGetStackForConstInstruction alloc = LLVMAllocaConstInstructionNodeGen.create(byteSize, alignment, type);
        return createGetStackSpace(context, type, alloc, byteSize);
    }

    @Override
    public LLVMExpressionNode createAlloca(LLVMContext context, Type type, int alignment) {
        int byteSize = context.getByteSize(type);
        LLVMGetStackForConstInstruction alloc = LLVMAllocaConstInstructionNodeGen.create(byteSize, alignment, type);
        return createGetStackSpace(context, type, alloc, byteSize);
    }

    @Override
    public LLVMExpressionNode createGetStackSpace(LLVMContext context, Type type, UniquesRegion uniquesRegion) {
        int alignment = context.getByteAlignment(type);
        int byteSize = context.getByteSize(type);
        UniqueSlot slot = uniquesRegion.addSlot(byteSize, alignment);
        LLVMGetStackForConstInstruction getStackSpace = LLVMGetUniqueStackSpaceInstructionNodeGen.create(byteSize, alignment, type, slot);
        return createGetStackSpace(context, type, getStackSpace, byteSize);
    }

    protected static LLVMExpressionNode createGetStackSpace(LLVMContext context, Type type, LLVMGetStackForConstInstruction getStackSpace, int byteSize) {
        if (type instanceof StructureType) {
            StructureType struct = (StructureType) type;
            final int[] offsets = new int[struct.getNumberOfElements()];
            final Type[] types = new Type[struct.getNumberOfElements()];
            int currentOffset = 0;
            for (int i = 0; i < struct.getNumberOfElements(); i++) {
                final Type elemType = struct.getElementType(i);

                if (!struct.isPacked()) {
                    currentOffset += context.getBytePadding(currentOffset, elemType);
                }

                offsets[i] = currentOffset;
                types[i] = elemType;
                currentOffset += context.getByteSize(elemType);
            }
            assert currentOffset <= byteSize : "currentOffset " + currentOffset + " vs. byteSize " + byteSize;
            getStackSpace.setTypes(types);
            getStackSpace.setOffsets(offsets);
        }
        return getStackSpace;
    }

    @Override
    public LLVMExpressionNode createAllocaArray(LLVMContext context, Type elementType, LLVMExpressionNode numElements, int alignment) {
        int byteSize = context.getByteSize(elementType);
        return LLVMAllocaInstructionNodeGen.create(numElements, byteSize, alignment, elementType);
    }

    @Override
    public VarargsAreaStackAllocationNode createVarargsAreaStackAllocation(LLVMContext context) {
        return LLVMNativeVarargsAreaStackAllocationNodeGen.create();
    }

    @Override
    public LLVMExpressionNode createInsertValue(LLVMExpressionNode resultAggregate, LLVMExpressionNode sourceAggregate, int size, long offset, LLVMExpressionNode valueToInsert, Type llvmType) {
        LLVMStoreNode store;
        if (llvmType instanceof PrimitiveType) {
            switch (((PrimitiveType) llvmType).getPrimitiveKind()) {
                case I1:
                    store = LLVMI1StoreNodeGen.create(null, null);
                    break;
                case I8:
                    store = LLVMI8StoreNodeGen.create(null, null);
                    break;
                case I16:
                    store = LLVMI16StoreNodeGen.create(null, null);
                    break;
                case I32:
                    store = LLVMI32StoreNodeGen.create(null, null);
                    break;
                case I64:
                    store = LLVMI64StoreNodeGen.create(null, null);
                    break;
                case FLOAT:
                    store = LLVMFloatStoreNodeGen.create(null, null);
                    break;
                case DOUBLE:
                    store = LLVMDoubleStoreNodeGen.create(null, null);
                    break;
                default:
                    throw new AssertionError(llvmType);
            }
        } else if (llvmType instanceof PointerType) {
            store = LLVMPointerStoreNodeGen.create(null, null);
        } else {
            throw new AssertionError(llvmType);
        }
        return LLVMInsertValueNodeGen.create(store, createMemMove(), size, offset, sourceAggregate, resultAggregate, valueToInsert);
    }

    @Override
    public LLVMExpressionNode createZeroNode(LLVMExpressionNode addressNode, int size) {
        return LLVMMemSetNodeGen.create(createMemSet(), addressNode, new LLVMI8LiteralNode((byte) 0), new LLVMI32LiteralNode(size), new LLVMI32LiteralNode(0), new LLVMI1LiteralNode(false), null);
    }

    @Override
    public LLVMExpressionNode createStructureConstantNode(LLVMContext context, Type structType, GetStackSpaceFactory getStackSpaceFactory, boolean packed, Type[] types,
                    LLVMExpressionNode[] constants) {
        int[] offsets = new int[types.length];
        LLVMStoreNode[] nodes = new LLVMStoreNode[types.length];
        int currentOffset = 0;
        LLVMExpressionNode getStackSpace = getStackSpaceFactory.createGetStackSpace(this, context, structType);
        for (int i = 0; i < types.length; i++) {
            Type resolvedType = types[i];
            if (!packed) {
                currentOffset += context.getBytePadding(currentOffset, resolvedType);
            }
            offsets[i] = currentOffset;
            int byteSize = context.getByteSize(resolvedType);
            nodes[i] = createMemoryStore(context, resolvedType);
            currentOffset += byteSize;
        }
        return StructLiteralNodeGen.create(offsets, nodes, constants, getStackSpace);
    }

    private LLVMStoreNode createMemoryStore(LLVMContext context, Type resolvedType) {
        if (resolvedType instanceof ArrayType || resolvedType instanceof StructureType) {
            int byteSize = context.getByteSize(resolvedType);
            return LLVMStructStoreNodeGen.create(null, createMemMove(), null, null, byteSize);
        } else if (resolvedType instanceof PrimitiveType) {
            switch (((PrimitiveType) resolvedType).getPrimitiveKind()) {
                case I8:
                    return LLVMI8StoreNodeGen.create(null, null);
                case I16:
                    return LLVMI16StoreNodeGen.create(null, null);
                case I32:
                    return LLVMI32StoreNodeGen.create(null, null);
                case I64:
                    return LLVMI64StoreNodeGen.create(null, null);
                case FLOAT:
                    return LLVMFloatStoreNodeGen.create(null, null);
                case DOUBLE:
                    return LLVMDoubleStoreNodeGen.create(null, null);
                case X86_FP80:
                    return LLVM80BitFloatStoreNodeGen.create(null, null);
                default:
                    throw new AssertionError(resolvedType);
            }
        } else if (resolvedType instanceof PointerType || Type.isFunctionOrFunctionPointer(resolvedType)) {
            return LLVMPointerStoreNodeGen.create(null, null);
        }
        throw new AssertionError(resolvedType);
    }

    @Override
    public LLVMStatementNode createBasicBlockNode(LLVMStatementNode[] statementNodes, LLVMControlFlowNode terminatorNode, int blockId,
                    String blockName) {
        return new LLVMBasicBlockNode(statementNodes, terminatorNode, blockId, blockName);
    }

    @Override
    public LLVMExpressionNode createFunctionBlockNode(FrameSlot exceptionValueSlot, List<? extends LLVMStatementNode> allFunctionNodes, UniquesRegion uniquesRegion, FrameSlot[][] beforeBlockNuller,
                    FrameSlot[][] afterBlockNuller, LLVMSourceLocation location, LLVMStatementNode[] copyArgumentsToFrame) {
        LLVMUniquesRegionAllocNode uniquesRegionAllocNode = LLVMUniquesRegionAllocNodeGen.create(uniquesRegion);
        return new LLVMDispatchBasicBlockNode(exceptionValueSlot, allFunctionNodes.toArray(new LLVMBasicBlockNode[allFunctionNodes.size()]), uniquesRegionAllocNode, beforeBlockNuller,
                        afterBlockNuller, location,
                        copyArgumentsToFrame);
    }

    @Override
    public RootNode createFunctionStartNode(LLVMContext context, LLVMExpressionNode functionBodyNode, SourceSection sourceSection, FrameDescriptor frame, FunctionDefinition functionHeader,
                    Source bcSource, LLVMSourceLocation location) {
        final String originalName = functionHeader.getSourceName();
        return new LLVMFunctionStartNode(sourceSection, context.getLanguage(), functionBodyNode, frame, functionHeader.getName(), functionHeader.getParameters().size(), originalName, bcSource,
                        location);
    }

    @Override
    public LLVMExpressionNode createInlineAssemblerExpression(LLVMContext context, ExternalLibrary library, String asmExpression, String asmFlags, LLVMExpressionNode[] args, Type[] argTypes,
                    Type retType, LLVMSourceLocation sourceSection) {
        Type[] retTypes = null;
        int[] retOffsets = null;
        if (retType instanceof StructureType) { // multiple out values
            assert args[1] instanceof LLVMGetStackForConstInstruction;
            LLVMGetStackForConstInstruction getStackSpace = (LLVMGetStackForConstInstruction) args[1];
            retTypes = getStackSpace.getTypes();
            retOffsets = getStackSpace.getOffsets();
        }

        LLVMInlineAssemblyRootNode assemblyRoot = InlineAssemblyParser.parseInlineAssembly(context.getLanguage(), sourceSection, asmExpression, asmFlags, argTypes, retType, retTypes, retOffsets);
        LLVMFunctionDescriptor asm = LLVMFunctionDescriptor.createDescriptor(context, "<asm>", new FunctionType(MetaType.UNKNOWN, new Type[0], false), -1);
        asm.define(library, new LLVMIRFunction(Truffle.getRuntime().createCallTarget(assemblyRoot), null));
        LLVMFunctionLiteralNode asmFunction = LLVMFunctionLiteralNodeGen.create(asm);

        return new LLVMCallNode(new FunctionType(MetaType.UNKNOWN, argTypes, false), asmFunction, args, sourceSection);
    }

    @Override
    public LLVMExpressionNode createFunctionArgNode(int i) {
        return LLVMArgNodeGen.create(i);
    }

    @Override
    public LLVMControlFlowNode createFunctionInvoke(FrameSlot resultLocation, LLVMExpressionNode functionNode, LLVMExpressionNode[] argNodes, FunctionType type,
                    int normalIndex, int unwindIndex, LLVMStatementNode normalPhiWriteNodes, LLVMStatementNode unwindPhiWriteNodes, LLVMSourceLocation source) {
        return new LLVMInvokeNode.LLVMFunctionInvokeNode(type, resultLocation, functionNode, argNodes, normalIndex, unwindIndex, normalPhiWriteNodes, unwindPhiWriteNodes,
                        source);
    }

    @Override
    public LLVMExpressionNode createLandingPad(LLVMExpressionNode allocateLandingPadValue, FrameSlot exceptionValueSlot, boolean cleanup, long[] clauseKinds,
                    LLVMExpressionNode[] entries, LLVMExpressionNode getStack) {

        LLVMLandingpadNode.LandingpadEntryNode[] landingpadEntries = new LLVMLandingpadNode.LandingpadEntryNode[entries.length];
        for (int i = 0; i < entries.length; i++) {
            if (clauseKinds[i] == 0) {
                // catch
                landingpadEntries[i] = getLandingpadCatchEntry(entries[i]);
            } else if (clauseKinds[i] == 1) {
                // filter
                landingpadEntries[i] = getLandingpadFilterEntry(entries[i]);
            } else {
                throw new IllegalStateException();
            }
        }
        return new LLVMLandingpadNode(getStack, allocateLandingPadValue, exceptionValueSlot, cleanup, landingpadEntries);
    }

    private static LLVMLandingpadNode.LandingpadEntryNode getLandingpadCatchEntry(LLVMExpressionNode exp) {
        return new LLVMLandingpadNode.LandingpadCatchEntryNode(exp);
    }

    private static LLVMLandingpadNode.LandingpadEntryNode getLandingpadFilterEntry(LLVMExpressionNode exp) {
        LLVMPointerArrayLiteralNode array = (LLVMPointerArrayLiteralNode) exp;
        LLVMToNativeNode[] types = array == null ? new LLVMToNativeNode[]{} : array.getValues();
        return new LLVMLandingpadNode.LandingpadFilterEntryNode(types);
    }

    @Override
    public LLVMControlFlowNode createResumeInstruction(FrameSlot exceptionValueSlot, LLVMSourceLocation source) {
        return new LLVMResumeNode(exceptionValueSlot, source);
    }

    @Override
    public LLVMExpressionNode createCompareExchangeInstruction(LLVMContext context, Type returnType, Type elementType, LLVMExpressionNode ptrNode, LLVMExpressionNode cmpNode,
                    LLVMExpressionNode newNode) {
        return LLVMCompareExchangeNodeGen.create(context.getByteSize(returnType),
                        context.getIndexOffset(1, (AggregateType) returnType), ptrNode, cmpNode, newNode);
    }

    @Override
    public LLVMExpressionNode createLLVMBuiltin(LLVMContext context, Symbol target, LLVMExpressionNode[] args, int callerArgumentCount, LLVMSourceLocation sourceSection) {
        /*
         * This LLVM Builtins are *not* function intrinsics. Builtins replace statements that look
         * like function calls but are actually LLVM intrinsics. An example is llvm.stackpointer.
         * Also, it is not possible to retrieve the functionpointer of such pseudo-call-targets.
         *
         * This builtins shall not be used for regular function intrinsification!
         */
        if (target instanceof FunctionDeclaration) {
            FunctionDeclaration declaration = (FunctionDeclaration) target;
            if (declaration.getName().startsWith("@llvm.")) {
                return getLLVMBuiltin(context, declaration, args, callerArgumentCount, sourceSection);
            } else if (declaration.getName().startsWith("@__builtin_")) {
                return getGccBuiltin(declaration, args, sourceSection);
            } else if (declaration.getName().equals("@polyglot_get_arg") || declaration.getName().equals("@truffle_get_arg")) {
                // this function accesses the frame directly
                // it must therefore not be hidden behind a call target
                return LLVMTruffleGetArgNodeGen.create(args[1], sourceSection);
            } else if (declaration.getName().equals("@polyglot_get_arg_count")) {
                // this function accesses the frame directly
                // it must therefore not be hidden behind a call target
                return LLVMTruffleGetArgCountNodeGen.create(sourceSection);
            } else if (declaration.getName().equals("@__divsc3")) {
                // this function allocates the result on the stack
                return new LLVMComplexDivSC(args[1], args[2], args[3], args[4]);
            }
        }
        return null;
    }

    protected LLVMExpressionNode getLLVMBuiltin(LLVMContext context, FunctionDeclaration declaration, LLVMExpressionNode[] args, int callerArgumentCount, LLVMSourceLocation sourceSection) {

        switch (declaration.getName()) {
            case "@llvm.memset.p0i8.i32":
            case "@llvm.memset.p0i8.i64":
                return LLVMMemSetNodeGen.create(createMemSet(), args[1], args[2], args[3], args[4], args[5], sourceSection);
            case "@llvm.assume":
                return LLVMAssumeNodeGen.create(args[1], sourceSection);
            case "@llvm.clear_cache": // STUB
            case "@llvm.donothing":
                return LLVMNoOpNodeGen.create(sourceSection);
            case "@llvm.prefetch":
                return LLVMPrefetchNodeGen.create(args[1], args[2], args[3], args[4], sourceSection);
            case "@llvm.ctlz.i8":
                return CountLeadingZeroesI8NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.ctlz.i16":
                return CountLeadingZeroesI16NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.ctlz.i32":
                return CountLeadingZeroesI32NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.ctlz.i64":
                return CountLeadingZeroesI64NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.memcpy.p0i8.p0i8.i64":
            case "@llvm.memcpy.p0i8.p0i8.i32":
                return LLVMMemCopyNodeGen.create(createMemMove(), args[1], args[2], args[3], args[4], args[5], sourceSection);
            case "@llvm.ctpop.i32":
                return CountSetBitsI32NodeGen.create(args[1], sourceSection);
            case "@llvm.ctpop.i64":
                return CountSetBitsI64NodeGen.create(args[1], sourceSection);
            case "@llvm.cttz.i8":
                return CountTrailingZeroesI8NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.cttz.i16":
                return CountTrailingZeroesI16NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.cttz.i32":
                return CountTrailingZeroesI32NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.cttz.i64":
                return CountTrailingZeroesI64NodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.trap":
                return LLVMTrapNodeGen.create(sourceSection);
            case "@llvm.bswap.i16":
                return LLVMByteSwapI16NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.i32":
                return LLVMByteSwapI32NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.i64":
                return LLVMByteSwapI64NodeGen.create(args[1], sourceSection);
            case "@llvm.bswap.v8i16":
                return LLVMByteSwapVI16NodeGen.create(8, args[1], sourceSection);
            case "@llvm.bswap.v16i16":
                return LLVMByteSwapVI16NodeGen.create(16, args[1], sourceSection);
            case "@llvm.bswap.v4i32":
                return LLVMByteSwapVI32NodeGen.create(4, args[1], sourceSection);
            case "@llvm.bswap.v8i32":
                return LLVMByteSwapVI32NodeGen.create(8, args[1], sourceSection);
            case "@llvm.bswap.v2i64":
                return LLVMByteSwapVI64NodeGen.create(2, args[1], sourceSection);
            case "@llvm.bswap.v4i64":
                return LLVMByteSwapVI64NodeGen.create(4, args[1], sourceSection);
            case "@llvm.memmove.p0i8.p0i8.i64":
                return LLVMMemMoveI64NodeGen.create(createMemMove(), args[1], args[2], args[3], args[4], args[5], sourceSection);
            case "@llvm.pow.f32":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.pow.f64":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.pow.f80":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.powi.f32":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.powi.f64":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.powi.f80":
                return LLVMPowNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.fabs.f32":
            case "@llvm.fabs.f64":
            case "@llvm.fabs.f80":
            case "@llvm.fabs.v2f64":
                return LLVMFAbsNodeGen.create(args[1], sourceSection);
            case "@llvm.returnaddress":
                return LLVMReturnAddressNodeGen.create(args[1], sourceSection);
            case "@llvm.lifetime.start.p0i8":
            case "@llvm.lifetime.start":
                return LLVMLifetimeStartNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.lifetime.end.p0i8":
            case "@llvm.lifetime.end":
                return LLVMLifetimeEndNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.invariant.start":
            case "@llvm.invariant.start.p0i8":
                return LLVMInvariantStartNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.invariant.end":
            case "@llvm.invariant.end.p0i8":
                return LLVMInvariantEndNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.stacksave":
                return LLVMStackSaveNodeGen.create(sourceSection);
            case "@llvm.stackrestore":
                return LLVMStackRestoreNodeGen.create(args[1], sourceSection);
            case "@llvm.frameaddress":
                return LLVMFrameAddressNodeGen.create(args[1], sourceSection);
            case "@llvm.va_start":
                return LLVMX86_64VAStartNodeGen.create(callerArgumentCount, sourceSection, createVarargsAreaStackAllocation(context), createMemMove(), args[1]);
            case "@llvm.va_end":
                return new LLVMX86_64BitVAEnd(args[1], sourceSection);
            case "@llvm.va_copy":
                return LLVMX86_64BitVACopyNodeGen.create(args[1], args[2], sourceSection, callerArgumentCount);
            case "@llvm.eh.sjlj.longjmp":
            case "@llvm.eh.sjlj.setjmp":
                throw new LLVMUnsupportedException(UnsupportedReason.SET_JMP_LONG_JMP);
            case "@llvm.dbg.declare":
                throw new IllegalStateException("@llvm.dbg.declare should be handled in the parser!");
            case "@llvm.dbg.value":
                throw new IllegalStateException("@llvm.dbg.value should be handled in the parser!");
            case "@llvm.eh.typeid.for":
                return new LLVMTypeIdForExceptionNode(args[1], sourceSection);
            case "@llvm.expect.i1": {
                boolean expectedValue = LLVMTypesGen.asBoolean(args[2].executeGeneric(null));
                LLVMExpressionNode actualValueNode = args[1];
                return LLVMExpectI1NodeGen.create(expectedValue, actualValueNode, sourceSection);
            }
            case "@llvm.expect.i32": {
                int expectedValue = LLVMTypesGen.asInteger(args[2].executeGeneric(null));
                LLVMExpressionNode actualValueNode = args[1];
                return LLVMExpectI32NodeGen.create(expectedValue, actualValueNode, sourceSection);
            }
            case "@llvm.expect.i64": {
                long expectedValue = LLVMTypesGen.asLong(args[2].executeGeneric(null));
                LLVMExpressionNode actualValueNode = args[1];
                return LLVMExpectI64NodeGen.create(expectedValue, actualValueNode, sourceSection);
            }
            case "@llvm.objectsize.i64.p0i8":
            case "@llvm.objectsize.i64":
                return LLVMI64ObjectSizeNodeGen.create(args[1], args[2], sourceSection);
            case "@llvm.copysign.f32":
            case "@llvm.copysign.f64":
            case "@llvm.copysign.f80":
                return LLVMCMathsIntrinsicsFactory.LLVMCopySignNodeGen.create(args[1], args[2], sourceSection);

            case "@llvm.uadd.with.overflow.i8":
            case "@llvm.uadd.with.overflow.i16":
            case "@llvm.uadd.with.overflow.i32":
            case "@llvm.uadd.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.UNSIGNED_ADD, getOverflowFieldOffset(context, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.usub.with.overflow.i8":
            case "@llvm.usub.with.overflow.i16":
            case "@llvm.usub.with.overflow.i32":
            case "@llvm.usub.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.UNSIGNED_SUB, getOverflowFieldOffset(context, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.umul.with.overflow.i8":
            case "@llvm.umul.with.overflow.i16":
            case "@llvm.umul.with.overflow.i32":
            case "@llvm.umul.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.UNSIGNED_MUL, getOverflowFieldOffset(context, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.sadd.with.overflow.i8":
            case "@llvm.sadd.with.overflow.i16":
            case "@llvm.sadd.with.overflow.i32":
            case "@llvm.sadd.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.SIGNED_ADD, getOverflowFieldOffset(context, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.ssub.with.overflow.i8":
            case "@llvm.ssub.with.overflow.i16":
            case "@llvm.ssub.with.overflow.i32":
            case "@llvm.ssub.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.SIGNED_SUB, getOverflowFieldOffset(context, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.smul.with.overflow.i8":
            case "@llvm.smul.with.overflow.i16":
            case "@llvm.smul.with.overflow.i32":
            case "@llvm.smul.with.overflow.i64":
                return LLVMArithmeticWithOverflowNodeGen.create(LLVMArithmetic.SIGNED_MUL, getOverflowFieldOffset(context, declaration), args[2], args[3], args[1], sourceSection);
            case "@llvm.exp2.f32":
            case "@llvm.exp2.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMExp2NodeGen.create(args[1], sourceSection);
            case "@llvm.sqrt.f32":
            case "@llvm.sqrt.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMSqrtNodeGen.create(args[1], sourceSection);
            case "@llvm.sin.f32":
            case "@llvm.sin.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMSinNodeGen.create(args[1], sourceSection);
            case "@llvm.cos.f32":
            case "@llvm.cos.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMCosNodeGen.create(args[1], sourceSection);
            case "@llvm.exp.f32":
            case "@llvm.exp.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMExpNodeGen.create(args[1], sourceSection);
            case "@llvm.log.f32":
            case "@llvm.log.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMLogNodeGen.create(args[1], sourceSection);
            case "@llvm.log2.f32":
            case "@llvm.log2.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMLog2NodeGen.create(args[1], sourceSection);
            case "@llvm.log10.f32":
            case "@llvm.log10.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMLog10NodeGen.create(args[1], sourceSection);
            case "@llvm.floor.f32":
            case "@llvm.floor.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMFloorNodeGen.create(args[1], sourceSection);
            case "@llvm.ceil.f32":
            case "@llvm.ceil.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMCeilNodeGen.create(args[1], sourceSection);
            case "@llvm.rint.f32":
            case "@llvm.rint.f64":
                return LLVMCMathsIntrinsicsFactory.LLVMRintNodeGen.create(args[1], sourceSection);
            case "@llvm.x86.sse.cvtss2si":
                return LLVMX86_ConversionFloatToIntNodeGen.create(args[1], sourceSection);
            case "@llvm.x86.sse2.cvtsd2si":
                return LLVMX86_ConversionDoubleToIntNodeGen.create(args[1], sourceSection);
            case "@llvm.x86.sse2.pmovmskb.128":
                return LLVMX86_Pmovmskb128NodeGen.create(args[1], sourceSection);

            default:
                throw new IllegalStateException("Missing LLVM builtin: " + declaration.getName());
        }
    }

    private static long getOverflowFieldOffset(LLVMContext context, FunctionDeclaration declaration) {
        return context.getIndexOffset(1, (AggregateType) declaration.getType().getReturnType());
    }

    protected LLVMExpressionNode getGccBuiltin(FunctionDeclaration declaration, LLVMExpressionNode[] args, LLVMSourceLocation sourceSection) {
        switch (declaration.getName()) {
            case "@__builtin_addcb":
            case "@__builtin_addcs":
            case "@__builtin_addc":
            case "@__builtin_addcl":
                return LLVMArithmeticWithOverflowAndCarryNodeGen.create(LLVMArithmetic.CARRY_ADD, args[1], args[2], args[3], args[4], sourceSection);
            case "@__builtin_subcb":
            case "@__builtin_subcs":
            case "@__builtin_subc":
            case "@__builtin_subcl":
                return LLVMArithmeticWithOverflowAndCarryNodeGen.create(LLVMArithmetic.CARRY_SUB, args[1], args[2], args[3], args[4], sourceSection);
            case "@__builtin_add_overflow":
                if (isZeroExtendArithmeticBuiltin(declaration)) {
                    throw new IllegalStateException("Missing GCC builtin: " + declaration.getName());
                } else {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.SIGNED_ADD, args[1], args[2], args[3], sourceSection);
                }
            case "@__builtin_sub_overflow":
                if (isZeroExtendArithmeticBuiltin(declaration)) {
                    throw new IllegalStateException("Missing GCC builtin: " + declaration.getName());
                } else {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.SIGNED_SUB, args[1], args[2], args[3], sourceSection);
                }
            case "@__builtin_mul_overflow":
                if (isZeroExtendArithmeticBuiltin(declaration)) {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.UNSIGNED_MUL, args[1], args[2], args[3], sourceSection);
                } else {
                    return GCCArithmeticNodeGen.create(LLVMArithmetic.SIGNED_MUL, args[1], args[2], args[3], sourceSection);
                }

            default:
                throw new IllegalStateException("Missing GCC builtin: " + declaration.getName());
        }
    }

    private static boolean isZeroExtendArithmeticBuiltin(FunctionDeclaration declaration) {
        final AttributesGroup group = declaration.getParameterAttributesGroup(0);
        if (group == null) {
            return false;
        }
        for (Attribute a : group.getAttributes()) {
            if (a instanceof KnownAttribute && ((KnownAttribute) a).getAttr() == Attribute.Kind.ZEROEXT) {
                return true;
            }
        }
        return false;
    }

    @Override
    public LLVMStatementNode createPhi(LLVMExpressionNode[] from, FrameSlot[] to, Type[] types) {
        if (to.length > 0) {
            if (to.length == 1) {
                return createFrameWrite(types[0], from[0], to[0], null);
            }
            LLVMWriteNode[] writes = new LLVMWriteNode[to.length];
            for (int i = 0; i < writes.length; i++) {
                writes[i] = createFrameWrite(types[i], null, to[i], null);
            }
            return new LLVMWritePhisNode(from, writes);
        }
        return null;
    }

    @Override
    public LLVMExpressionNode createCopyStructByValue(LLVMContext context, Type type, GetStackSpaceFactory getStackSpaceFactory, LLVMExpressionNode parameterNode) {
        LLVMExpressionNode getStackSpaceNode = getStackSpaceFactory.createGetStackSpace(this, context, type);
        return LLVMStructByValueNodeGen.create(createMemMove(), getStackSpaceNode, parameterNode, context.getByteSize(type));
    }

    @Override
    public LLVMExpressionNode createVarArgCompoundValue(int length, int alignment, LLVMExpressionNode parameterNode) {
        return LLVMVarArgCompoundAddressNodeGen.create(parameterNode, length, alignment);
    }

    private static LLVMDebugBuilder getDebugDynamicValueBuilder(boolean isDeclaration) {
        if (isDeclaration) {
            return LLVMDebugBuilder.NATIVE_DECLARATION;
        } else {
            return LLVMDebugBuilder.NATIVE_VALUE;
        }
    }

    @Override
    public LLVMStatementNode createDebugValueUpdate(boolean isDeclaration, LLVMExpressionNode valueRead, FrameSlot targetSlot, LLVMExpressionNode containerRead, int partIndex, int[] clearParts) {
        final LLVMDebugBuilder builder = getDebugDynamicValueBuilder(isDeclaration);
        if (partIndex < 0 || clearParts == null) {
            return LLVMDebugWriteNodeFactory.SimpleWriteNodeGen.create(builder, targetSlot, valueRead);
        } else {
            return LLVMDebugWriteNodeFactory.AggregateWriteNodeGen.create(builder, partIndex, clearParts, containerRead, valueRead);
        }
    }

    @Override
    public LLVMFrameValueAccess createDebugFrameValue(FrameSlot slot, boolean isDeclaration) {
        final TruffleLanguage.ContextReference<LLVMContext> contextRef = LLVMLanguage.getLLVMContextReference();
        final LLVMDebugValue.Builder builder = getDebugDynamicValueBuilder(isDeclaration).createBuilder(contextRef);
        return new LLVMFrameValueAccessImpl(slot, builder);
    }

    @Override
    public LLVMStatementNode createDebugValueInit(FrameSlot targetSlot, int[] offsets, int[] lengths) {
        if (offsets == null || lengths == null) {
            return null;
        } else {
            return LLVMDebugInitNodeFactory.AggregateInitNodeGen.create(targetSlot, offsets, lengths);
        }
    }

    @Override
    public LLVMDebugObjectBuilder createDebugStaticValue(LLVMExpressionNode valueNode) {
        // TODO (jkreindl) this is correct, globals as containers
        final LLVMDebugValue.Builder toDebugNode = LLVMToDebugValueNodeGen.LLVMToStaticDebugValueNodeGen.create(LLVMLanguage.getLLVMContextReference());
        return createDebugValue(valueNode, toDebugNode);
    }

    @Override
    public LLVMDebugObjectBuilder createDebugDynamicValue(LLVMExpressionNode valueNode) {
        // TODO (jkreindl) always treat globals as pointer to memory
        final LLVMDebugValue.Builder toDebugNode = LLVMToDebugValueNodeGen.LLVMToDynamicDebugValueNodeGen.create(LLVMLanguage.getLLVMContextReference());
        return createDebugValue(valueNode, toDebugNode);
    }

    private static LLVMDebugObjectBuilder createDebugValue(LLVMExpressionNode valueNode, LLVMDebugValue.Builder toDebugNode) {
        if (valueNode == null) {
            return LLVMDebugObjectBuilder.UNAVAILABLE;
        }

        Object value;
        try {
            value = valueNode.executeGeneric(null);
        } catch (Throwable t) {
            // constant values should not need frame access
            value = null;
        }

        if (value != null) {
            return LLVMDebugSimpleObjectBuilder.create(toDebugNode, value);
        } else {
            return LLVMDebugObjectBuilder.UNAVAILABLE;
        }
    }

    @Override
    public LLVMStatementNode registerSourceType(FrameSlot valueSlot, LLVMSourceType type) {
        LLVMSourceType actual = type.getActualType();
        if (actual instanceof LLVMSourcePointerType) {
            // only pointer types can contain foreign values
            LLVMSourceType base = ((LLVMSourcePointerType) actual).getBaseType();
            LLVMInteropType interopType = LLVMInteropType.fromSourceType(base);
            if (interopType != null) {
                return new LLVMSetInteropTypeNode(valueSlot, interopType);
            }
        }
        return null;
    }

    @Override
    public LLVMMemMoveNode createMemMove() {
        return NativeProfiledMemMoveNodeGen.create();
    }

    @Override
    public LLVMAllocateStringNode createAllocateString() {
        return NativeAllocateStringNodeGen.create();
    }

    @Override
    public LLVMMemSetNode createMemSet() {
        return NativeMemSetNodeGen.create();
    }

    private static LLVMExpressionNode[] createDoubleLiteralNodes(int nrElements, double value) {
        LLVMExpressionNode[] doubleZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            doubleZeroInits[i] = new LLVMDoubleLiteralNode(value);
        }
        return doubleZeroInits;
    }

    private static LLVMExpressionNode[] createFloatLiteralNodes(int nrElements, float value) {
        LLVMExpressionNode[] floatZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            floatZeroInits[i] = new LLVMFloatLiteralNode(value);
        }
        return floatZeroInits;
    }

    private static LLVMExpressionNode[] createI64LiteralNodes(int nrElements, long value) {
        LLVMExpressionNode[] i64ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i64ZeroInits[i] = new LLVMI64LiteralNode(value);
        }
        return i64ZeroInits;
    }

    private static LLVMExpressionNode[] createI32LiteralNodes(int nrElements, int value) {
        LLVMExpressionNode[] i32ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i32ZeroInits[i] = new LLVMI32LiteralNode(value);
        }
        return i32ZeroInits;
    }

    private static LLVMExpressionNode[] createI16LiteralNodes(int nrElements, short value) {
        LLVMExpressionNode[] i16ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i16ZeroInits[i] = new LLVMI16LiteralNode(value);
        }
        return i16ZeroInits;
    }

    private static LLVMExpressionNode[] createI8LiteralNodes(int nrElements, byte value) {
        LLVMExpressionNode[] i8ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i8ZeroInits[i] = new LLVMI8LiteralNode(value);
        }
        return i8ZeroInits;
    }

    private static LLVMExpressionNode[] createI1LiteralNodes(int nrElements, boolean value) {
        LLVMExpressionNode[] i1ZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            i1ZeroInits[i] = new LLVMI1LiteralNode(value);
        }
        return i1ZeroInits;
    }

    private static LLVMExpressionNode[] createNullAddressLiteralNodes(int nrElements) {
        LLVMExpressionNode[] addressZeroInits = new LLVMExpressionNode[nrElements];
        for (int i = 0; i < nrElements; i++) {
            addressZeroInits[i] = new LLVMNativePointerLiteralNode(LLVMNativePointer.createNull());
        }
        return addressZeroInits;
    }

    @Override
    public LLVMLoadNode createLoadNode(LLVMInteropType.ValueKind kind) {
        switch (kind) {
            case I1:
                return LLVMI1LoadNodeGen.create(null);
            case I8:
                return LLVMI8LoadNodeGen.create(null);
            case I16:
                return LLVMI16LoadNodeGen.create(null);
            case I32:
                return LLVMI32LoadNodeGen.create(null);
            case I64:
                return LLVMI64LoadNodeGen.create(null);
            case FLOAT:
                return LLVMFloatLoadNodeGen.create(null);
            case DOUBLE:
                return LLVMDoubleLoadNodeGen.create(null);
            case POINTER:
                return LLVMPointerDirectLoadNodeGen.create(null);
            default:
                throw new IllegalStateException("unexpected interop kind " + kind);
        }
    }

    private static LLVMExpressionNode createLoad(Type resultType, LLVMExpressionNode loadTarget, int bits) {
        if (resultType instanceof PrimitiveType) {
            switch (((PrimitiveType) resultType).getPrimitiveKind()) {
                case I1:
                    return LLVMI1LoadNodeGen.create(loadTarget);
                case I8:
                    return LLVMI8LoadNodeGen.create(loadTarget);
                case I16:
                    return LLVMI16LoadNodeGen.create(loadTarget);
                case I32:
                    return LLVMI32LoadNodeGen.create(loadTarget);
                case I64:
                    return LLVMI64LoadNodeGen.create(loadTarget);
                case FLOAT:
                    return LLVMFloatLoadNodeGen.create(loadTarget);
                case DOUBLE:
                    return LLVMDoubleLoadNodeGen.create(loadTarget);
                case X86_FP80:
                    return LLVM80BitFloatDirectLoadNodeGen.create(loadTarget);
                default:
                    throw new AssertionError(resultType);
            }
        } else if (resultType instanceof VariableBitWidthType) {
            return LLVMIVarBitDirectLoadNodeGen.create(loadTarget, bits);
        } else if (Type.isFunctionOrFunctionPointer(resultType)) {
            return LLVMFunctionDirectLoadNodeGen.create(loadTarget);
        } else if (resultType instanceof StructureType || resultType instanceof ArrayType) {
            return LLVMStructDirectLoadNodeGen.create(loadTarget);
        } else if (resultType instanceof PointerType) {
            if (loadTarget instanceof LLVMAccessGlobalVariableStorageNode) {
                return new LLVMGlobalDirectLoadNode(((LLVMAccessGlobalVariableStorageNode) loadTarget).getDescriptor());
            } else {
                return LLVMPointerDirectLoadNodeGen.create(loadTarget);
            }
        } else {
            throw new AssertionError(resultType);
        }
    }

    @Override
    public LLVMStoreNode createStoreNode(LLVMInteropType.ValueKind kind) {
        switch (kind) {
            case I1:
                return LLVMI1StoreNodeGen.create(null, null);
            case I8:
                return LLVMI8StoreNodeGen.create(null, null);
            case I16:
                return LLVMI16StoreNodeGen.create(null, null);
            case I32:
                return LLVMI32StoreNodeGen.create(null, null);
            case I64:
                return LLVMI64StoreNodeGen.create(null, null);
            case FLOAT:
                return LLVMFloatStoreNodeGen.create(null, null);
            case DOUBLE:
                return LLVMDoubleStoreNodeGen.create(null, null);
            case POINTER:
                return LLVMPointerStoreNodeGen.create(null, null);
            default:
                throw new IllegalStateException("unexpected interop kind " + kind);
        }
    }

    private LLVMStatementNode createStore(LLVMExpressionNode pointerNode, LLVMExpressionNode valueNode, Type type, int size, LLVMSourceLocation source) {
        if (type instanceof PrimitiveType) {
            switch (((PrimitiveType) type).getPrimitiveKind()) {
                case I1:
                    return LLVMI1StoreNodeGen.create(source, pointerNode, valueNode);
                case I8:
                    return LLVMI8StoreNodeGen.create(source, pointerNode, valueNode);
                case I16:
                    return LLVMI16StoreNodeGen.create(source, pointerNode, valueNode);
                case I32:
                    return LLVMI32StoreNodeGen.create(source, pointerNode, valueNode);
                case I64:
                    return LLVMI64StoreNodeGen.create(source, pointerNode, valueNode);
                case FLOAT:
                    return LLVMFloatStoreNodeGen.create(source, pointerNode, valueNode);
                case DOUBLE:
                    return LLVMDoubleStoreNodeGen.create(source, pointerNode, valueNode);
                case X86_FP80:
                    return LLVM80BitFloatStoreNodeGen.create(source, pointerNode, valueNode);
                default:
                    throw new AssertionError(type);
            }
        } else if (type instanceof VariableBitWidthType) {
            return LLVMIVarBitStoreNodeGen.create(source, pointerNode, valueNode);
        } else if (Type.isFunctionOrFunctionPointer(type)) {
            return LLVMFunctionStoreNodeGen.create(source, pointerNode, valueNode);
        } else if (type instanceof StructureType || type instanceof ArrayType) {
            return LLVMStructStoreNodeGen.create(source, createMemMove(), pointerNode, valueNode, size);
        } else if (type instanceof PointerType) {
            if (pointerNode instanceof LLVMAccessGlobalVariableStorageNode) {
                return LLVMGlobalVariableStoreNodeGen.create(((LLVMAccessGlobalVariableStorageNode) pointerNode).getDescriptor(), source, valueNode);
            } else {
                return LLVMPointerStoreNodeGen.create(source, pointerNode, valueNode);
            }
        } else if (type instanceof VectorType) {
            VectorType vectorType = (VectorType) type;
            return LLVMStoreVectorNodeGen.create(source, vectorType, pointerNode, valueNode);
        } else {
            throw new AssertionError(type);
        }
    }
}
