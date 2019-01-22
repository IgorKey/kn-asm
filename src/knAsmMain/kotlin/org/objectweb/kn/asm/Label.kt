/***
 * ASM: a very small and fast Java bytecode manipulation framework
 * Copyright (c) 2000-2011 INRIA, France Telecom
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of the copyright holders nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.objectweb.kn.asm

/**
 * A label represents a position in the bytecode of a method. Labels are used
 * for jump, goto, and switch instructions, and for try catch blocks. A label
 * designates the *instruction* that is just after. Note however that there
 * can be other elements between a label and the instruction it designates (such
 * as other labels, stack map frames, line numbers, etc.).
 *
 * @author Eric Bruneton
 */
// ------------------------------------------------------------------------
// Constructor
// ------------------------------------------------------------------------

/**
 * Constructs a new label.
 */
class Label {

    /**
     * Field used to associate user information to a label. Warning: this field
     * is used by the ASM tree package. In order to use it with the ASM tree
     * package you must override the
     * [org.objectweb.asm.tree.MethodNode.getLabelNode] method.
     */
    var info: Any? = null

    /**
     * Flags that indicate the status of this label.
     *
     * @see .DEBUG
     *
     * @see .RESOLVED
     *
     * @see .RESIZED
     *
     * @see .PUSHED
     *
     * @see .TARGET
     *
     * @see .STORE
     *
     * @see .REACHABLE
     *
     * @see .JSR
     *
     * @see .RET
     */
    internal var status: Int = 0

    /**
     * The line number corresponding to this label, if known.
     */
    internal var line: Int = 0

    /**
     * The position of this label in the code, if known.
     */
    internal var position: Int = 0

    /**
     * Number of forward references to this label, times two.
     */
    private var referenceCount: Int = 0

    /**
     * Informations about forward references. Each forward reference is
     * described by two consecutive integers in this array: the first one is the
     * position of the first byte of the bytecode instruction that contains the
     * forward reference, while the second is the position of the first byte of
     * the forward reference itself. In fact the sign of the first integer
     * indicates if this reference uses 2 or 4 bytes, and its absolute value
     * gives the position of the bytecode instruction. This array is also used
     * as a bitset to store the subroutines to which a basic block belongs. This
     * information is needed in {@linked MethodWriter#visitMaxs}, after all
     * forward references have been resolved. Hence the same array can be used
     * for both purposes without problems.
     */
    private var srcAndRefPositions: IntArray? = null

    // ------------------------------------------------------------------------

    /*
     * Fields for the control flow and data flow graph analysis algorithms (used
     * to compute the maximum stack size or the stack map frames). A control
     * flow graph contains one node per "basic block", and one edge per "jump"
     * from one basic block to another. Each node (i.e., each basic block) is
     * represented by the Label object that corresponds to the first instruction
     * of this basic block. Each node also stores the list of its successors in
     * the graph, as a linked list of Edge objects.
     *
     * The control flow analysis algorithms used to compute the maximum stack
     * size or the stack map frames are similar and use two steps. The first
     * step, during the visit of each instruction, builds information about the
     * state of the local variables and the operand stack at the end of each
     * basic block, called the "output frame", <i>relatively</i> to the frame
     * state at the beginning of the basic block, which is called the "input
     * frame", and which is <i>unknown</i> during this step. The second step, in
     * {@link MethodWriter#visitMaxs}, is a fix point algorithm that computes
     * information about the input frame of each basic block, from the input
     * state of the first basic block (known from the method signature), and by
     * the using the previously computed relative output frames.
     *
     * The algorithm used to compute the maximum stack size only computes the
     * relative output and absolute input stack heights, while the algorithm
     * used to compute stack map frames computes relative output frames and
     * absolute input frames.
     */

    /**
     * Start of the output stack relatively to the input stack. The exact
     * semantics of this field depends on the algorithm that is used.
     *
     * When only the maximum stack size is computed, this field is the number of
     * elements in the input stack.
     *
     * When the stack map frames are completely computed, this field is the
     * offset of the first output stack element relatively to the top of the
     * input stack. This offset is always negative or null. A null offset means
     * that the output stack must be appended to the input stack. A -n offset
     * means that the first n output stack elements must replace the top n input
     * stack elements, and that the other elements must be appended to the input
     * stack.
     */
    internal var inputStackTop: Int = 0

    /**
     * Maximum height reached by the output stack, relatively to the top of the
     * input stack. This maximum is always positive or null.
     */
    internal var outputStackMax: Int = 0

    /**
     * Information about the input and output stack map frames of this basic
     * block. This field is only used when [ClassWriter.COMPUTE_FRAMES]
     * option is used.
     */
    internal var frame: Frame? = null

    /**
     * The successor of this label, in the order they are visited. This linked
     * list does not include labels used for debug info only. If
     * [ClassWriter.COMPUTE_FRAMES] option is used then, in addition, it
     * does not contain successive labels that denote the same bytecode position
     * (in this case only the first label appears in this list).
     */
    internal var successor: Label? = null

    /**
     * The successors of this node in the control flow graph. These successors
     * are stored in a linked list of [Edge] objects, linked to each
     * other by their [Edge.next] field.
     */
    internal lateinit var successors: Edge

    /**
     * The next basic block in the basic block stack. This stack is used in the
     * main loop of the fix point algorithm used in the second step of the
     * control flow analysis algorithms. It is also used in
     * [.visitSubroutine] to avoid using a recursive method.
     *
     * @see MethodWriter.visitMaxs
     */
    internal var next: Label? = null

    // ------------------------------------------------------------------------
    // Methods to compute offsets and to manage forward references
    // ------------------------------------------------------------------------

    /**
     * Returns the offset corresponding to this label. This offset is computed
     * from the start of the method's bytecode. *This method is intended for
     * [Attribute] sub classes, and is normally not needed by class
     * generators or adapters.*
     *
     * @return the offset corresponding to this label.
     * @throws IllegalStateException
     * if this label is not resolved yet.
     */
    val offset: Int
        get() {
            if (status and RESOLVED == 0) {
                throw IllegalStateException(
                        "Label offset position has not been resolved yet")
            }
            return position
        }

    /**
     * Returns the first label of the series to which this label belongs. For an
     * isolated label or for the first label in a series of successive labels,
     * this method returns the label itself. For other labels it returns the
     * first label of the series.
     *
     * @return the first label of the series to which this label belongs.
     */
    internal val first: Label
        get() = if (!ClassReader.FRAMES || frame == null) this else frame!!.owner!!

    /**
     * Puts a reference to this label in the bytecode of a method. If the
     * position of the label is known, the offset is computed and written
     * directly. Otherwise, a null offset is written and a new forward reference
     * is declared for this label.
     *
     * @param owner
     * the code writer that calls this method.
     * @param out
     * the bytecode of the method.
     * @param source
     * the position of first byte of the bytecode instruction that
     * contains this label.
     * @param wideOffset
     * <tt>true</tt> if the reference must be stored in 4 bytes, or
     * <tt>false</tt> if it must be stored with 2 bytes.
     * @throws IllegalArgumentException
     * if this label has not been created by the given code writer.
     */
    internal fun put(owner: MethodWriter, out: ByteVector, source: Int,
                     wideOffset: Boolean) {
        if (status and RESOLVED == 0) {
            if (wideOffset) {
                addReference(-1 - source, out.length)
                out.putInt(-1)
            } else {
                addReference(source, out.length)
                out.putShort(-1)
            }
        } else {
            if (wideOffset) {
                out.putInt(position - source)
            } else {
                out.putShort(position - source)
            }
        }
    }

    /**
     * Adds a forward reference to this label. This method must be called only
     * for a true forward reference, i.e. only if this label is not resolved
     * yet. For backward references, the offset of the reference can be, and
     * must be, computed and stored directly.
     *
     * @param sourcePosition
     * the position of the referencing instruction. This position
     * will be used to compute the offset of this forward reference.
     * @param referencePosition
     * the position where the offset for this forward reference must
     * be stored.
     */
    private fun addReference(sourcePosition: Int,
                             referencePosition: Int) {
        if (srcAndRefPositions == null) {
            srcAndRefPositions = IntArray(6)
        }
        if (referenceCount >= srcAndRefPositions!!.size) {
            val a = IntArray(srcAndRefPositions!!.size + 6)
            srcAndRefPositions!!.copyInto(a,0,0, srcAndRefPositions!!.size)

            srcAndRefPositions = a
        }
        srcAndRefPositions!![referenceCount++] = sourcePosition
        srcAndRefPositions!![referenceCount++] = referencePosition
    }

    /**
     * Resolves all forward references to this label. This method must be called
     * when this label is added to the bytecode of the method, i.e. when its
     * position becomes known. This method fills in the blanks that where left
     * in the bytecode by each forward reference previously added to this label.
     *
     * @param owner
     * the code writer that calls this method.
     * @param position
     * the position of this label in the bytecode.
     * @param data
     * the bytecode of the method.
     * @return <tt>true</tt> if a blank that was left for this label was to
     * small to store the offset. In such a case the corresponding jump
     * instruction is replaced with a pseudo instruction (using unused
     * opcodes) using an unsigned two bytes offset. These pseudo
     * instructions will need to be replaced with true instructions with
     * wider offsets (4 bytes instead of 2). This is done in
     * [MethodWriter.resizeInstructions].
     * @throws IllegalArgumentException
     * if this label has already been resolved, or if it has not
     * been created by the given code writer.
     */
    internal fun resolve(owner: MethodWriter, position: Int,
                         data: ByteArray): Boolean {
        var needUpdate = false
        this.status = this.status or RESOLVED
        this.position = position
        var i = 0
        while (i < referenceCount) {
            val source = srcAndRefPositions!![i++]
            var reference = srcAndRefPositions!![i++]
            val offset: Int
            if (source >= 0) {
                offset = position - source
                if (offset <Short.MIN_VALUE || offset > Short.MAX_VALUE) {
                    /*
                     * changes the opcode of the jump instruction, in order to
                     * be able to find it later (see resizeInstructions in
                     * MethodWriter). These temporary opcodes are similar to
                     * jump instruction opcodes, except that the 2 bytes offset
                     * is unsigned (and can therefore represent values from 0 to
                     * 65535, which is sufficient since the size of a method is
                     * limited to 65535 bytes).
                     */
                    val opcode = data[reference - 1].toInt() and 0xFF
                    if (opcode <= Opcodes.JSR) {
                        // changes IFEQ ... JSR to opcodes 202 to 217
                        data[reference - 1] = (opcode + 49).toByte()
                    } else {
                        // changes IFNULL and IFNONNULL to opcodes 218 and 219
                        data[reference - 1] = (opcode + 20).toByte()
                    }
                    needUpdate = true
                }
                data[reference++] = offset.ushr(8).toByte()
                data[reference] = offset.toByte()
            } else {
                offset = position + source + 1
                data[reference++] = offset.ushr(24).toByte()
                data[reference++] = offset.ushr(16).toByte()
                data[reference++] = offset.ushr(8).toByte()
                data[reference] = offset.toByte()
            }
        }
        return needUpdate
    }

    // ------------------------------------------------------------------------
    // Methods related to subroutines
    // ------------------------------------------------------------------------

    /**
     * Returns true is this basic block belongs to the given subroutine.
     *
     * @param id
     * a subroutine id.
     * @return true is this basic block belongs to the given subroutine.
     */
    internal fun inSubroutine(id: Long): Boolean {
        return if (status and VISITED != 0) {
            srcAndRefPositions!![id.ushr(32).toInt()] and id.toInt() != 0
        } else false
    }

    /**
     * Returns true if this basic block and the given one belong to a common
     * subroutine.
     *
     * @param block
     * another basic block.
     * @return true if this basic block and the given one belong to a common
     * subroutine.
     */
    internal fun inSameSubroutine(block: Label): Boolean {
        if (status and VISITED == 0 || block.status and VISITED == 0) {
            return false
        }
        for (i in srcAndRefPositions!!.indices) {
            if (srcAndRefPositions!![i] and block.srcAndRefPositions!![i] != 0) {
                return true
            }
        }
        return false
    }

    /**
     * Marks this basic block as belonging to the given subroutine.
     *
     * @param id
     * a subroutine id.
     * @param nbSubroutines
     * the total number of subroutines in the method.
     */
    internal fun addToSubroutine(id: Long, nbSubroutines: Int) {
        if (status and VISITED == 0) {
            status = status or VISITED
            srcAndRefPositions = IntArray((nbSubroutines - 1) / 32 + 1)
        }
        srcAndRefPositions!![id.ushr(32).toInt()] = srcAndRefPositions!![id.ushr(32).toInt()] or id.toInt()
    }

    /**
     * Finds the basic blocks that belong to a given subroutine, and marks these
     * blocks as belonging to this subroutine. This method follows the control
     * flow graph to find all the blocks that are reachable from the current
     * block WITHOUT following any JSR target.
     *
     * @param JSR
     * a JSR block that jumps to this subroutine. If this JSR is not
     * null it is added to the successor of the RET blocks found in
     * the subroutine.
     * @param id
     * the id of this subroutine.
     * @param nbSubroutines
     * the total number of subroutines in the method.
     */
    internal fun visitSubroutine(JSR: Label?, id: Long, nbSubroutines: Int) {
        // user managed stack of labels, to avoid using a recursive method
        // (recursivity can lead to stack overflow with very large methods)
        var stack: Label? = this
        while (stack != null) {
            // removes a label l from the stack
            val l = stack
            stack = l.next
            l.next = null

            if (JSR != null) {
                if (l.status and VISITED2 != 0) {
                    continue
                }
                l.status = l.status or VISITED2
                // adds JSR to the successors of l, if it is a RET block
                if (l.status and RET != 0) {
                    if (!l.inSameSubroutine(JSR)) {
                        val e = Edge()
                        e.info = l.inputStackTop
                        e.successor = JSR.successors.successor
                        e.next = l.successors
                        l.successors = e
                    }
                }
            } else {
                // if the l block already belongs to subroutine 'id', continue
                if (l.inSubroutine(id)) {
                    continue
                }
                // marks the l block as belonging to subroutine 'id'
                l.addToSubroutine(id, nbSubroutines)
            }
            // pushes each successor of l on the stack, except JSR targets
            var e: Edge? = l.successors
            while (e != null) {
                // if the l block is a JSR block, then 'l.successors.next' leads
                // to the JSR target (see {@link #visitJumpInsn}) and must
                // therefore not be followed
                if (l.status and Companion.JSR == 0 || e !== l.successors.next) {
                    // pushes e.successor on the stack if it not already added
                    if (e.successor!!.next == null) {
                        e.successor!!.next = stack
                        stack = e.successor
                    }
                }
                e = e.next
            }
        }
    }

    // ------------------------------------------------------------------------
    // Overriden Object methods
    // ------------------------------------------------------------------------

    /**
     * Returns a string representation of this label.
     *
     * @return a string representation of this label.
     */
    override fun toString(): String {
        return "L" + "myOwns.sd.as"
    }

    companion object {

        /**
         * Indicates if this label is only used for debug attributes. Such a label
         * is not the start of a basic block, the target of a jump instruction, or
         * an exception handler. It can be safely ignored in control flow graph
         * analysis algorithms (for optimization purposes).
         */
        internal val DEBUG = 1

        /**
         * Indicates if the position of this label is known.
         */
        internal val RESOLVED = 2

        /**
         * Indicates if this label has been updated, after instruction resizing.
         */
        internal val RESIZED = 4

        /**
         * Indicates if this basic block has been pushed in the basic block stack.
         * See [visitMaxs][MethodWriter.visitMaxs].
         */
        internal val PUSHED = 8

        /**
         * Indicates if this label is the target of a jump instruction, or the start
         * of an exception handler.
         */
        internal val TARGET = 16

        /**
         * Indicates if a stack map frame must be stored for this label.
         */
        internal val STORE = 32

        /**
         * Indicates if this label corresponds to a reachable basic block.
         */
        internal val REACHABLE = 64

        /**
         * Indicates if this basic block ends with a JSR instruction.
         */
        internal val JSR = 128

        /**
         * Indicates if this basic block ends with a RET instruction.
         */
        internal val RET = 256

        /**
         * Indicates if this basic block is the start of a subroutine.
         */
        internal val SUBROUTINE = 512

        /**
         * Indicates if this subroutine basic block has been visited by a
         * visitSubroutine(null, ...) call.
         */
        internal val VISITED = 1024

        /**
         * Indicates if this subroutine basic block has been visited by a
         * visitSubroutine(!null, ...) call.
         */
        internal val VISITED2 = 2048
    }
}
