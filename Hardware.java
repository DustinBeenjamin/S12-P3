
import java.util.ArrayList;

/*
 * AUTHOR:      Benjamin Dustin
 * COURSE:      Computer Architecture
 * ASSIGNMENT:  PROJECT 2 - PIPELINES S12 SIMULATION
 * DESCRIPTION: ->
 *      This class stores the information about a pipelined s12 processor at a single point in time of the simulatoin.
 *      Create a Hardware object to begin the simulation. For each clock of the sim, use the clone method of this class on the current hardware object.
 *      Then call the clock method on the clone, passing the original hardware object as an argument. This will cause the clocked hardware object to 
 *      update its state based on the previous object. All latching will happen based on the previous hardware object. But, the combinational logic
 *      might not use the previous state as it needs to settle.
 *
 *      Since the Hardware is pipelined, a single signal may have different values in each stage. Therefore, the variables in this class use a naming 
 *      convention to distinguish which stage the signal belongs to. For example...
 * 
 *                  -pc_enable_1    represents the state of the pc enable control signal in stage 1
 *                  -pc_enable_2                                                     ... in stage 2
 *                  -pc_enable_3                                                     ... in stage 3   
 *                  -pc_enable_4_1                                                   ... in stage 4_1 (stage 4 but before the accumulator)
 *                  -pc_enable_4_2                                                   ... in stage 4_2 (stage 4 but  after the accumulator)
 *                  -pc_enable_5                                                     ... in stage 5
 * 
 *      This bulky naming convention happens because all control signals and data need to be propogated from stage 1 down the pipe. So if you see
 *      a signal that has a ..._4_1 but not a ..._4_2 or a ..._5, its because the signal only needed to be propogated to stage 4_1
 * 
 *      Notably, there are two stages within the stage 4. This is because the accumulator is treated as a register, which needs to be clocked to update
 *      The program counter is also treated like a register that can be enabled, or disabled for stalling.
 *                  
 * 
 */

public class Hardware implements Cloneable{
    
    public int[] memory;                                                    //integer array that hold program and data memory
    public int executeHalt = 0;                                             //if the program should execute a halt
    public Simulator simulation;                                            //a reference, used to tell simulation if a halt is encountered
    public ArrayList<Stage5State> stage5States = new ArrayList<>();         //a collection of objects that summarize stage 5 after a clock
    
    public int en_1 = 0;                                                    //enables the register after stage 1                    
    public int en_2 = 0;                                                    //                 ... after stage 2      
    public int en_3 = 0;                                                    //                            ...  3
    public int en_4_1 = 0;                                                  //                            ... etc
    public int en_4_2 = 0;

    /////
    //STAGE 1 SIGNALS
    /////
    public int pc = 0;                          //the 'actual' program counter
    public int pc_mux_1 = 0;                    //select line of a mux fed into the pc register
    public int pc_enable_1 = 0;                 //enables the pc register (disable for stalling)
    public int accum_mux_1 = 0;                 //select line of mux that feeds the accumulator
    public int alu_op_1 = 0;                    //select line of a mux that feeds the alu
    public int accum_enable_1 = 0;              //enables the accumulator
    public int address_mux_1 = 0;               //select line of a mux that feeds the memory write block (direct or indirect address)
    public int write_enable_1 = 0;              //enables the write block (write block is treated as combinational, not sequential)
    public int branch_enable_1 = 0;             //enables branching, and prevents the default 0's held in registers at startup from triggering a branch
    public int raw_instruction_1 = 0;           //instruction at program counter

    //////
    //STAGE 2 SIGNALS
    //////
    public int pc_2 = 0;                        //not a control signal, but propogated through pipe to help generate instruction trace from stage 5
    public int accum_mux_2 = 0; 
    public int alu_op_2 = 0;
    public int accum_enable_2 = 0;
    public int address_mux_2 = 0;
    public int write_enable_2 = 0;
    public int branch_enable_2 = 0;
    public int raw_instruction_2 = 0;
    public int first_lookup_2 = 0;              //the value of memory, using the lower 8 bits of raw_instruction_2 as an address

    /////
    //STAGE 3 SIGNALS
    /////
    public int pc_3 = 0;
    public int accum_mux_3 = 0;
    public int alu_op_3 = 0;
    public int accum_enable_3 = 0;
    public int address_mux_3 = 0;
    public int write_enable_3 = 0;
    public int branch_enable_3 = 0;
    public int raw_instruction_3 = 0;
    public int first_lookup_3 = 0;
    public int second_lookup_3 = 0;             //the value of memory, using the lower 8 bits of first_lookup_3 as an address


    /////
    ///STAGE 4_1 SIGNALS
    /////
    public int pc_4_1 = 0;
    public int accum_mux_4_1 = 0;
    public int alu_op_4_1 = 0;
    public int accum_enable_4_1 = 0;
    public int address_mux_4_1 = 0;
    public int write_enable_4_1 = 0;
    public int branch_enable_4_1 = 0;
    public int raw_instruction_4_1 = 0;
    public int first_lookup_4_1 = 0;
    public int second_lookup_4_1 = 0;

    
    /////
    ///STAGE 4_2 SIGNALS
    /////
    public int pc_4_2 = 0;
    public int address_mux_4_2 = 0;
    public int write_enable_4_2 = 0;
    public int branch_enable_4_2 = 0;
    public int raw_instruction_4_2 = 0;
    public int first_lookup_4_2 = 0;
    public int alt_pc_4_2 = 0;                  //the alternate program counter if you should branch
    public int accum_4_2 = 0;                   //the real time value of the accumulator
    public int zero_4_2 = 0;                    //combinational flag, indicates if accumulator is zero
    public int negative_4_2 = 0;                //combinational flag, indicates if accumulator is negative


    //The '_stageNumber' naming convention does not apply to these signals. 
    public int flush_pipe = 0;                  //combinational flag, indicates next clock should flush the pipe
    public int flush_pipe_latch_1 = 0;          //latched version of flush_pipe
    public int flush_pipe_latch_2 = 0;          //latched version of flush_pipe_latch_1    
    public int flush_pipe_latch_3 = 0;          //               ... flush_pipe_latch_2
    public int flush_pipe_latch_4 = 0;          //               ... etc
    public int flush_pipe_latch_5 = 0;
    public int latched_flush_pipe_present = 0;  //combinational flag, indicates if flush pipe is latched into any register
    public int temp = 0;                        //combinational flag, indicates if flush pipe is latched into registers _1 through _4

    /////
    ///STAGE 5 SIGNALS
    /////
    public int pc_5 = 0;
    public int address_mux_5 = 0;
    public int write_enable_5 = 0;
    public int raw_instruction_5 = 0;
    public int first_lookup_5 = 0;
    public int accum_5 = 0;
    public int next_store_fsm_state_5 = 0;      //combinational flag, indicates what the next state of an fsm responsible for managing stalls


    /////
    ///SIGNALS USED BY THE STORE FSM
    /////
    public int store_fsm_state = 0;             //combinational flag, indicates the current state of the fsm responsible for managing stalls
    public int store_counter = 0;               //used by fsm to count cycles spent in a state
    public int store_enable_shift_register = 0; //___>


    /////
    ///SMART STORE SIGNALS 
    ///// 
    public ArrayList<Integer> busy_addresses = new ArrayList<>();
    public ArrayList<Integer> address_timers = new ArrayList<>();
    public int min_busy_cycles = 4;
    public boolean address_conflict;



    //When a stall happens, all the registers (except the first) need to be become disabled after the instructions before the stall finish. 
    //Then, each clock should cause the next register to be enabled.
    //This caused the enable register signals to 'walk' or 'cascase' from stage1 to stage5. Instead of manually controlling all the enables, the 
    //fsm responsible for storing only enables the first register. That enable gets piplined, creating the 'cascading' enable effect.
    //Conversely, it means that driving the first enable low causes the cascaded enable signal to die out from left to right. This allows signal that came
    //before the stall to finish.

    //Constructor (ONLY USE FOR THE FIRST HARDWARE OBJECT. ALL SEQUENTIAL OBJECTS SHOULD BE COPIES OF THE PREVIOUS)
    public Hardware(int init_pc, int init_accum, int[] memory, Simulator simulation) {
        //Set the inital conditions. 
        this.simulation = simulation;
        this.memory = memory;
        this.pc = init_pc;
        this.accum_4_2 = init_accum;
        this.raw_instruction_1 = memory[init_pc];
        this.first_lookup_2 = memory[0];
        this.second_lookup_3 = memory[0];
        this.pc_mux_1 = 0x00;
        
        //Set initial signals required by store fsm
        this.store_fsm_state = StoreFsmState.RESTARTING.getCode();
        //is the first instructoin a store or a storei?
        boolean isStore = ((memory[init_pc] >> 8) == Opcodes.STORE.getCode());              
        boolean isStoreI = ((memory[init_pc] >> 8) == Opcodes.STOREI.getCode());            
        //set the next state, which changes logic (starts with stalling)
        next_store_fsm_state_5 = (isStore || isStoreI) ? StoreFsmState.STALLING.getCode() : StoreFsmState.CASCADING.getCode();
        this.en_1 = 0x01;
        this.pc_enable_1 = 0x01;
        store_counter = 0;

    }

    //Updates the Program Counter (SEQUENTIAL LOGIC)
    public void updatePc(Hardware p) {

        //Dont latch a new pc if you should halt
        if (executeHalt == 0x01) {
            return;
        }

        //flushing the pipe indicates you should branch
        if ((p.pc_enable_1 == 0x01) || (flush_pipe == 0x01)) {
            pc = (p.pc_mux_1 == 0) ? (p.pc + 1) : p.alt_pc_4_2;
        }
    }

    //Generate control signals only related to storing
    public void generatStoreControlSignals (Hardware p) {
        
        store_fsm_state = p.next_store_fsm_state_5;

        //Assign control signals based on current state
        switch (store_fsm_state) {
            case (0x01):
                //RESTARTING
                store_counter = 0;
                next_store_fsm_state_5 = (address_conflict) ? StoreFsmState.STALLING.getCode() : StoreFsmState.CASCADING.getCode();
                pc_enable_1 = (address_conflict) ? 0x00 : 0x01;
                //next clock should only latch a new pc
                en_1 = (address_conflict) ? 0x00 : 0x01;
                en_2 = 0x00;
                en_3 = 0x00;
                en_4_1 = 0x00;
                en_4_2 = 0x00;
                break;
            case (0x02):
                //CASCADING
                pc_enable_1 = (address_conflict) ? 0x00 : 0x01;
                en_1 = (address_conflict) ? 0x00 : 0x01;
                store_counter = 0;
                next_store_fsm_state_5 = (address_conflict) ? StoreFsmState.STALLING.getCode() : StoreFsmState.CASCADING.getCode();
                break;
            case (0x04):
                //STALLING
                //dont update pc
                pc_enable_1 = 0x00;
                en_1 = 0x00;
                //count how many states have been stalled
                store_counter++;
                next_store_fsm_state_5 = (store_counter <= 2) ? StoreFsmState.STALLING.getCode() : StoreFsmState.CASCADING.getCode();
                break;
        }

        //Overide the next fsm state if there is a pipe flush
        next_store_fsm_state_5 = (flush_pipe == 0x01) ? StoreFsmState.RESTARTING.getCode() : next_store_fsm_state_5;
        pc_enable_1 = (flush_pipe == 0x01) ? 0x01 : pc_enable_1;
        en_1 = (flush_pipe == 0x01) ? 0x01 : en_1;
    }

    //The halting logic wait for a halt to arrive at stage 4_2 (after accumulator)
    //However, a branch is executed after it leaves 4_2 (if the conditions are correct)
    //Therefore, you must also check if there is a latched flush pipe to determine if the halt should actually trigger
    public void haltLogic() {
        executeHalt = (((raw_instruction_4_2 >> 8) == 0xF) && (latched_flush_pipe_present == 0x00)) ? 0x01 : 0x00;
    }

    //Generate control signals that are not related to halting
    public void generateControlSignals(Hardware p) {

        int opcodeBinary = (raw_instruction_1 >> 8);

        //Default all signals to 0, set non zeros in the switch that follows
        accum_mux_1 = 0x00;
        accum_enable_1 = 0x00;
        alu_op_1 = 0x00;
        address_mux_1 = 0x00;
        write_enable_1 = 0x00;

        //Drive signals if they are non zero, or not dont cares
        switch (opcodeBinary) {
            case (0b0100):
                //LOAD
                accum_mux_1 = 0x01;
                accum_enable_1 = 0x01;
                break;
            case (0b0110):
                //LOADI
                accum_mux_1 = 0x02;
                accum_enable_1 = 0x01;
                break;
            case (0b0101):
                //STORE
                write_enable_1 = 0x01;
                break;
            case (0b0111):
                //STOREI
                write_enable_1 = 0x01;
                address_mux_1 = 0x01;
                break;
            case (0b1010):
                //ADD
                accum_enable_1 = 0x01;
                alu_op_1 = 0x02;
                break;
            case (0b1011):
                //SUB
                accum_enable_1 = 0x01;
                alu_op_1 = 0x03;
                break;
            case (0b1000):
                //AND
                accum_enable_1 = 0x01;
                break;
            case (0b1001):
                //OR
                accum_enable_1 = 0x01;
                alu_op_1 = 0x01;
                break;
            case (0b0001):
                //JN
                branch_enable_1 = 0x01;
                break;
            case (0b0000):
                //JMP
                branch_enable_1 = 0x01;
                break;
            case (0b0010):
                //JZ
                branch_enable_1 = 0x01;
                break;
            case (0b1111):
                //HALT
                // accum_mux_1 = 0x00;
                // accum_enable_1 = 0x00;
                // alu_op_1 = 0x00;
                // address_mux_1 = 0x00;
                // write_enable_1 = 0x00;
                break;
            

            default:
                System.out.println("FUCK. Tried to generate control signals, but recieved unknown opcode 0b" + Integer.toString(opcodeBinary, 2));
                break;
        }
    }

    //Call this method to update 'clock' or 'update' this Hardware snapshot. You must pass a reference to the previous Hardware snapshot
    //as the argument p
    public void clock(Hardware p) {

        //Dont clock if you need to halt
        if (executeHalt == 0x01) {
            simulation.endSimulation();
            return;
        }

        //Update Stage 1
            //->Handle memory writes
            //->Handle sequential logic (latches)
            //->Handle combinational logic

        updateMemory(p);
        updatePc(p); 
        latch(p);
        updateAccum(p);
        combinationalLogic(p);
        branchLogic();
        haltLogic();
        generateControlSignals(p);
        generatStoreControlSignals(p);
        saveStage5State();

    }

    //Sets control signals that determines if you should branch
    public void branchLogic() {
        //Determine if there was recently a flush pipe
        latched_flush_pipe_present = ((flush_pipe_latch_1 == (0x01)) || (flush_pipe_latch_2 == (0x01)) || (flush_pipe_latch_3 == (0x01)) || (flush_pipe_latch_4 == (0x01))) ? 0x01 : 0x00;
        temp = ((flush_pipe_latch_1 == (0x01)) || (flush_pipe_latch_2 == (0x01)) || (flush_pipe_latch_3 == (0x01))) ? 0x01 : 0x00;
        
        //Make sure the default 0 values of register dont trigger a jump (jump has opcode 0000)
        boolean jumpEnabled = ((branch_enable_4_2 == 0x01) && (latched_flush_pipe_present == 0x00));

        //See if you should jump based on accumulator status
        boolean jumpN   = (((raw_instruction_4_2 >> 8) == 0x01) && (negative_4_2 == 0x01));
        boolean jumpZ   = (((raw_instruction_4_2 >> 8) == 0x02) && (zero_4_2 == 0x01));
        boolean jump    = ((raw_instruction_4_2 >> 8) == 0x00); 

        //The final decision to branch or not
        boolean executeBranch = (jumpEnabled && (jumpN || jumpZ || jump));

        //If you branch, you need to flush the pipe
        flush_pipe = (executeBranch) ? 0x01 : 0x00;

        //If you branch, you need the mux feeding program counter to provide that alt address, not the pc + 1
        pc_mux_1 = (flush_pipe == 0x01) ? 0x01 : 0x00;

    }

    //Updates sequential logic, based on previous hardware snapshot 'p'
    public void latch(Hardware p) {

        //Dont latch anything if you should halt
        if (executeHalt == 0x01) {
            return;
        }
        
        //Update Signals Latched into Stage 2
        if (p.en_1 == 0x01) {
            raw_instruction_2 = p.raw_instruction_1;
            pc_2 = p.pc;
            
            accum_mux_2 = p.accum_mux_1;
            alu_op_2 = p.alu_op_1;
            accum_enable_2 = p.accum_enable_1;
            address_mux_2 = p.address_mux_1;
            write_enable_2 = p.write_enable_1;
            branch_enable_2 = p.branch_enable_1;
        }

        //Update Signals Latched into Stage 3
        if (p.en_2  == 0x01) {
            raw_instruction_3 = p.raw_instruction_2;
            first_lookup_3 = p.first_lookup_2;
            pc_3 = p.pc_2;

            accum_mux_3 = p.accum_mux_2;
            alu_op_3 = p.alu_op_2;
            accum_enable_3 = p.accum_enable_2;
            address_mux_3 = p.address_mux_2;
            write_enable_3 = p.write_enable_2;
            branch_enable_3 = p.branch_enable_2;
        }

        //Update Signals Latched into Stage 4.1
        if (p.en_3  == 0x01) {
            raw_instruction_4_1 = p.raw_instruction_3;
            first_lookup_4_1 = p.first_lookup_3;
            second_lookup_4_1 = p.second_lookup_3;
            pc_4_1 = p.pc_3;

            accum_mux_4_1 = p.accum_mux_3;
            alu_op_4_1 = p.alu_op_3;
            accum_enable_4_1 = p.accum_enable_3;
            address_mux_4_1 = p.address_mux_3;
            write_enable_4_1 = p.write_enable_3;
            branch_enable_4_1 = p.branch_enable_3;
        }

        //Update Signals Latched into Stage 4.2
        if (p.en_4_1  == 0x01) {
            raw_instruction_4_2 = p.raw_instruction_4_1;
            first_lookup_4_2 = p.first_lookup_4_1;
            pc_4_2 = p.pc_4_1;

            address_mux_4_2 = p.address_mux_4_1;
            write_enable_4_2 = p.write_enable_4_1;
            branch_enable_4_2 = p.branch_enable_4_1;
        }

        //The enable of the flush pipe latching should not be gated
        flush_pipe_latch_1 = p.flush_pipe;
        flush_pipe_latch_2 = p.flush_pipe_latch_1;
        flush_pipe_latch_3 = p.flush_pipe_latch_2;
        flush_pipe_latch_4 = p.flush_pipe_latch_3;
        flush_pipe_latch_5 = p.flush_pipe_latch_4;

        //Update Signals Latched into Stage 5
        if (p.en_4_2  == 0x01) {
            raw_instruction_5 = p.raw_instruction_4_2;
            first_lookup_5 = p.first_lookup_4_2;
            pc_5 = p.pc_4_2;

            address_mux_5 = p.address_mux_4_2;
            write_enable_5 = p.write_enable_4_2;

            accum_5 = p.accum_4_2;
        }

        //Cascade the register enables
        en_2 = p.en_1;
        en_3 = p.en_2;
        en_4_1 = p.en_3;
        en_4_2 = p.en_4_1;
    }

    //Primarily updates data signals that are generated by a state, instead of latched
    public void combinationalLogic(Hardware p) {
        raw_instruction_1 = memory[pc];
        first_lookup_2 = memory[raw_instruction_2 & 0x0FF];
        second_lookup_3 = memory[first_lookup_3 & 0x0FF];
        zero_4_2 = (accum_4_2 == 0x00) ? 0x01 : 0x00;
        negative_4_2 = ((accum_4_2 >> 11) == 0x01) ? 0x01 : 0x00;
        alt_pc_4_2 = (raw_instruction_4_2 & 0x0FF);

        //SMART STORE

        //Increase the timers for all the busy addresses by 1.
        for (int i = 0; i < address_timers.size(); i++) {
            Integer oldValue = address_timers.remove(i);
            address_timers.add(i, oldValue + 1);
        }
        

        int last_index = address_timers.size();
        //Make sure to remove an adress from busy addresses if its timer has expired.
        for (int i = 0; i < last_index; i++) {
            Integer val = address_timers.get(i);
            if (val > min_busy_cycles) {
                address_timers.remove(i);
                busy_addresses.remove(i);
                last_index--;
                i--;
            }
        }

        //Determine if the current instuction is a store conflift
        if (busy_addresses.indexOf(getAddressBinary(raw_instruction_1)) != -1) {
            address_conflict = true;
        } else {
            address_conflict = false;
        }


        //If the new instruction is store or storei, save the address and append it to the list of busy addresses.
        //Also, start a timer for it to keep track of how long ago it was.
        if ((isSaveOp(raw_instruction_1)) && (pc_enable_1 == 0x01)) {
            int new_instruction_opcode = getOpcodeBinary(raw_instruction_1);
            //If its a store instruction, then the busy address is the lower 8 bits.
            //But, if its a storei instruction, the busy address is the lower 8 bits of memory at the lower 8 bits of the instruction
            int first_busy_address = getAddressBinary(raw_instruction_1);
            busy_addresses.add(first_busy_address);
            int second_busy_address = getAddressBinary(memory[getAddressBinary(first_busy_address)]);
            busy_addresses.add(second_busy_address);
            address_timers.add(0);
            address_timers.add(0);
        }

    }

    public boolean isSaveOp(int instruction) {
        int opcodeBinary = (instruction >> 8);
        return (opcodeBinary == Opcodes.STORE.getCode()) || (opcodeBinary == Opcodes.STOREI.getCode());
    }

    public int getOpcodeBinary(int instruction) {
        return (instruction >> 8);
    }

    public int getAddressBinary(int instruction) {
        return (instruction & 0xFF);
    }

    //Responsible for writing to memory
    public void updateMemory(Hardware p) {
        dereferenceMemory();
        if (p.write_enable_5 == 0x01) {
            int address = (p.address_mux_5 == 0) ? (p.raw_instruction_5 & 0xFF) : (p.first_lookup_5 & 0xFF);
            memory[address] = p.accum_5;
            // System.out.println(String.format("Wrote %d to address %d.", p.accum_5, address));
        }
    }

    //Create a copy of the previous hardwares memory
    public void dereferenceMemory() {
        int[] temp_memory = new int[256];
        int i = 0;
        for (int m : memory) {
            temp_memory[i] = m;
            i++;
        }
        this.memory = temp_memory;
    }

    //Update the accumulator
    public void updateAccum(Hardware p){

        //Dont latch a new pc if you should halt
        if (executeHalt == 0x01) {
            return;
        }

        //Accumulator should only latch if the stage is enabled (not stalled), there is a operation that needs to update it (unlike store or branching), and the pipe is
        //not being flushed
        if ((p.accum_enable_4_1 == 0x01) && (p.en_4_1 == 0x01) && (p.temp == 0x00) && (p.flush_pipe == 0x00)) {
            int alu_result = 0;

            //Calculate ALU ouput
            switch (p.alu_op_4_1) {
                case (0x00):
                    //Logical And
                    alu_result = (p.accum_4_2 & p.first_lookup_4_1) & 0xFFF;  
                    break;
                case (0x01):
                    //Logical Or
                    alu_result = (p.accum_4_2 | p.first_lookup_4_1) & 0xFFF;
                    break;
                case (0x02):
                    //Math Add
                    alu_result = (p.accum_4_2 + p.first_lookup_4_1) & 0xFFF;
                    break;
                case (0x03):
                    //Math Sub
                    alu_result = (p.accum_4_2 - p.first_lookup_4_1) & 0xFFF;
                    break;
                default:
                    System.out.println("FUCK. Tried to update accum, but alu op had a value of " + p.alu_op_4_1);
                    System.exit(0);            
            }

            //Determine what should  latch into accumulator on next clock
            switch (p.accum_mux_4_1) {
                case (0x00):
                    //ALU Result
                    accum_4_2 = alu_result; 
                    break;
                case (0x01):
                    //First Memory Lookup
                    accum_4_2 = p.first_lookup_4_1;
                    break;
                case (0x02):
                    //Second Memory Lookup
                    accum_4_2 = p.second_lookup_4_1;
                    break;
                case (0x03):
                    //Accumulator Ouput
                    accum_4_2 = p.accum_4_2;
                    break;
                default:
                    System.out.println("FUCK. Tried to update accum, but accum mux had a value of " + p.accum_mux_4_1);            
            }
        }
    }

    @Override
    //Use this to generate a copy of a hardware object, then clock the copy
    public Hardware clone() {
        try {
            Hardware copy = (Hardware) super.clone();
            copy.memory = memory.clone(); // deep copy of array
            return copy;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    //Saves a snapshot of stage 5 on each clock cycle, used later to generate metrics
    public void saveStage5State(){
        
        stage5States.add(new Stage5State(pc_5, Opcodes.fromCode((raw_instruction_5 >> 8))));

        //HALT
        if ((executeHalt == 0x01) && (raw_instruction_4_2 >> 8) == Opcodes.HALT.getCode()) {
            stage5States.add(new Stage5State(pc_4_2, Opcodes.HALT));
        }
    }
}
